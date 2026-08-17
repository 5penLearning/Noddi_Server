package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.dto.QaAnswerStreamEventDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseStatus;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class QaAnswerStreamService {


     //네트워크 지연이나 OpenAI 응답 지연을 고려해 한 SSE 연결을 최대 30분 동안 유지한다.
    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    /*
     * 완료 또는 실패 이벤트를 5분 동안 메모리에 남긴다.
     *
     * AI 답변이 끝나는 순간과 사용자가 SSE에 접속하는 순간이 겹치더라도
     * 마지막 완료/실패 이벤트를 놓치지 않게 하기 위한 보관 시간이다.
     */
    private static final long TERMINATED_SESSION_RETENTION_MILLIS =
            5 * 60 * 1000L;

    /*
     * questionId별 스트리밍 상태를 저장한다.
     *
     * 하나의 질문을 여러 프로젝트 멤버가 동시에 볼 수 있으므로
     * 질문 하나에 여러 SseEmitter가 연결될 수 있다.
     *
     * ConcurrentHashMap을 사용하는 이유는 다음 작업들이 서로 다른
     * 스레드에서 동시에 실행될 수 있기 때문이다.
     *
     * - HTTP 요청 스레드: SSE 구독
     * - @Async 스레드: AI 답변 생성 및 chunk 전달
     * - 스케줄러 스레드: 종료된 세션 정리
     */
    private final ConcurrentMap<Long, StreamSession> sessions =
            new ConcurrentHashMap<>();

    /**
     * AI 답변 생성 작업을 시작할 때 호출한다.
     *
     * 이전 실패 또는 복구 작업에서 남아 있을 수 있는 답변 조각과
     * 종료 이벤트를 초기화한다.
     *
     * 기존 SSE 연결 목록은 유지한다. 따라서 자동 재시도가 시작되더라도
     * 사용자는 같은 연결에서 새 답변을 받을 수 있다.
     */
    public void start(Long questionId, int attempt) {
        StreamSession session = sessions.computeIfAbsent(
                questionId,
                ignored -> new StreamSession()
        );

        //여러 스레드가 동시에 실행하지 못하게 세션별로 잠금
        synchronized (session) {
            // 이전 시도의 늦은 시작 요청이 현재 재시도 세션을 되돌리지 못하게 한다.
            if (attempt < session.activeAttempt) {
                return;
            }

            session.activeAttempt = attempt;
            session.content.setLength(0);
            session.terminalEvent = null;
            session.terminatedAt = null;
        }
    }

    /**
     * 클라이언트의 SSE 연결을 생성한다.
     *
     * currentStatus, answerId, answerContent는 나중에 구독 서비스가
     * DB에서 조회한 현재 질문/답변 정보를 전달한다.
     *
     * 서버가 재시작되어 메모리 세션이 사라졌더라도 DB 상태가 ANSWERED라면
     * 저장된 최종 답변을 즉시 내려줄 수 있어야 하기 때문이다.
     */
    public SseEmitter subscribe(
            Long questionId,
            QaStatus currentStatus,
            Long answerId,
            String answerContent
    ) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);

        /*
         * DB에서 이미 완료 또는 실패한 상태를 확인했다면
         * 장시간 연결을 유지할 필요 없이 현재 상태를 보내고 종료한다.
         */
        if (currentStatus == QaStatus.ANSWERED) {
            sendEvent(
                    emitter,
                    QaAnswerStreamEventDto.connected(questionId, QaResponseStatus.from(currentStatus, false))
            );
            sendEvent(
                    emitter,
                    QaAnswerStreamEventDto.completed(
                            questionId,
                            answerId,
                            answerContent
                    )
            );
            emitter.complete();
            return emitter;
        }

        if (currentStatus == QaStatus.MANUAL_REQUIRED) {
            sendEvent(
                    emitter,
                    QaAnswerStreamEventDto.connected(questionId, QaResponseStatus.TEAM_ANSWER_PENDING)
            );
            sendEvent(
                    emitter,
                    QaAnswerStreamEventDto.teamAnswerPending(
                            questionId,
                            answerId,
                            answerContent
                    )
            );
            emitter.complete();
            return emitter;
        }

        StreamSession session = sessions.computeIfAbsent(
                questionId,
                ignored -> new StreamSession()
        );

        registerCallbacks(questionId, session, emitter);

        /*
         * subscribe와 publishChunk가 동시에 실행되더라도 이벤트 순서가
         * 뒤집히지 않도록 동일한 session 객체를 기준으로 동기화한다.
         *
         * 반드시 emitter를 먼저 등록하고 snapshot을 보내야 이후에 생성되는
         * chunk가 현재 연결에도 전달된다.
         */
        synchronized (session) {
            session.emitters.add(emitter);

            boolean connected = sendEvent(
                    emitter,
                    QaAnswerStreamEventDto.connected(questionId, QaResponseStatus.from(currentStatus, true))
            );

            if (!connected) {
                session.emitters.remove(emitter);
                return emitter;
            }

            /*
             * DB 상태를 읽은 직후 AI 작업이 완료되는 경쟁 상황이 있을 수 있다.
             *
             * DB에서는 아직 PROCESSING을 읽었더라도 메모리 세션에 종료 이벤트가
             * 등록됐다면 해당 이벤트를 즉시 보내고 연결을 종료한다.
             */
            if (session.terminalEvent != null) {
                sendEvent(emitter, session.terminalEvent);
                session.emitters.remove(emitter);
                emitter.complete();
                return emitter;
            }

            /*
             * AI 답변 생성 도중 늦게 접속했다면 지금까지 누적된 전체 답변을
             * snapshot으로 먼저 전달한다.
             *
             * 이후 생성되는 텍스트부터 chunk 이벤트로 이어서 받게 된다.
             */
            if (!session.content.isEmpty()) {
                sendEvent(
                        emitter,
                        QaAnswerStreamEventDto.snapshot(
                                questionId,
                                session.content.toString(),
                                session.activeAttempt
                        )
                );
            }
        }

        return emitter;
    }

    /**
     * AI가 생성한 답변 조각 하나를 누적하고 모든 구독자에게 전달한다.
     */
    public void publishChunk(Long questionId, int attempt, String delta) {
        /*
         * 공백 문자열도 AI 답변 구성에 필요할 수 있다.
         *
         * 따라서 isBlank()를 사용하면 안 된다.
         * 예를 들어 " 출시일" 앞의 공백을 제거하면 문장이 붙어버릴 수 있다.
         */
        if (delta == null || delta.isEmpty()) {
            return;
        }

        StreamSession session = sessions.computeIfAbsent(
                questionId,
                ignored -> new StreamSession()
        );

        synchronized (session) {
            /*
             * AI 답변이 종료되었거나 이전 재시도에서 뒤늦게 도착한 chunk는 무시한다.
             */
            if (session.terminalEvent != null || session.activeAttempt != attempt) {
                return;
            }

            session.content.append(delta);

            broadcast(
                    session,
                    QaAnswerStreamEventDto.chunk(questionId, delta, attempt)
            );
        }
    }

    /**
     * 최종 답변이 DB에 저장된 이후 호출한다.
     *
     * 반드시 DB 저장이 성공한 뒤 호출해야 한다. DB 저장 전에 COMPLETED를
     * 보내면 프론트가 피드를 다시 조회했을 때 답변을 찾지 못할 수 있다.
     */
    public void publishCompleted(
            Long questionId,
            Long answerId,
            String content
    ) {
        StreamSession session = sessions.computeIfAbsent(
                questionId,
                ignored -> new StreamSession()
        );

        QaAnswerStreamEventDto completedEvent =
                QaAnswerStreamEventDto.completed(
                        questionId,
                        answerId,
                        content
                );

        terminate(session, completedEvent);
    }

    /**
     * AI 생성 또는 답변 저장이 실패했을 때 호출한다.
     */
    public void publishFailed(Long questionId) {
        StreamSession session = sessions.computeIfAbsent(
                questionId,
                ignored -> new StreamSession()
        );

        terminate(
                session,
                QaAnswerStreamEventDto.failed(questionId)
        );
    }

    /** 자동 재시도가 예정된 실패를 알리되 SSE 연결은 유지한다. */
    public void publishRetrying(Long questionId, int attempt) {
        StreamSession session = sessions.computeIfAbsent(
                questionId,
                ignored -> new StreamSession()
        );

        synchronized (session) {
            if (session.terminalEvent == null) {
                broadcast(session, QaAnswerStreamEventDto.retrying(questionId, attempt));
            }
        }
    }

    /** AI 최종 실패와 대상 팀 직접 답변 대기 상태를 전송하고 연결을 종료한다. */
    public void publishTeamAnswerPending(Long questionId, Long answerId, String content) {
        StreamSession session = sessions.computeIfAbsent(
                questionId,
                ignored -> new StreamSession()
        );
        terminate(
                session,
                QaAnswerStreamEventDto.teamAnswerPending(questionId, answerId, content)
        );
    }

    /**
     * 종료 이벤트를 모든 구독자에게 전달하고 SSE 연결을 닫는다.
     *
     * 종료 이벤트 자체는 잠시 메모리에 보관한다. 완료 시점과 구독 시점이
     * 겹치는 사용자가 마지막 이벤트를 놓치지 않도록 하기 위해서다.
     */
    private void terminate(
            StreamSession session,
            QaAnswerStreamEventDto terminalEvent
    ) {
        synchronized (session) {
            /*
             * complete와 fail이 경쟁하더라도 최초 종료 이벤트만 인정한다.
             */
            if (session.terminalEvent != null) {
                return;
            }

            session.terminalEvent = terminalEvent;
            session.terminatedAt = Instant.now();

            for (SseEmitter emitter : session.emitters) {
                sendEvent(emitter, terminalEvent);
                emitter.complete();
            }

            session.emitters.clear();
        }
    }

    /**
     * 현재 질문을 구독 중인 모든 SSE 연결에 이벤트를 전달한다.
     *
     * 전송에 실패한 emitter는 이미 연결이 끊긴 것으로 보고 목록에서 제거한다.
     */
    private void broadcast(
            StreamSession session,
            QaAnswerStreamEventDto event
    ) {
        session.emitters.removeIf(emitter -> !sendEvent(emitter, event));
    }

    /**
     * DTO를 실제 SSE 형식으로 변환해 전송한다.
     *
     * 예:
     *
     * event: chunk
     * data: {"type":"chunk","questionId":1,...}
     */
    private boolean sendEvent(
            SseEmitter emitter,
            QaAnswerStreamEventDto event
    ) {
        try {
            emitter.send(
                    SseEmitter.event()
                            .name(event.type().getEventName())
                            .data(event)
            );
            return true;
        } catch (IOException | IllegalStateException exception) {
            /*
             * 브라우저 종료나 네트워크 단절은 정상적으로 발생할 수 있으므로
             * 전체 AI 생성 작업을 실패 처리하지 않는다.
             */
            log.debug(
                    "Q&A SSE event send failed. questionId={}, type={}",
                    event.questionId(),
                    event.type(),
                    exception
            );
            emitter.complete();
            return false;
        }
    }

    /**
     * 타임아웃, 네트워크 오류, 정상 종료가 발생하면 emitter를 제거한다.
     *
     * 콜백을 등록하지 않으면 끊어진 연결이 Map에 계속 남아 메모리 누수가
     * 발생할 수 있다.
     */
    private void registerCallbacks(
            Long questionId,
            StreamSession session,
            SseEmitter emitter
    ) {
        emitter.onCompletion(() -> removeEmitter(questionId, session, emitter));

        emitter.onTimeout(() -> {
            removeEmitter(questionId, session, emitter);
            emitter.complete();
        });

        emitter.onError(exception ->
                removeEmitter(questionId, session, emitter)
        );
    }

    private void removeEmitter(
            Long questionId,
            StreamSession session,
            SseEmitter emitter
    ) {
        session.emitters.remove(emitter);

        log.debug(
                "Q&A SSE emitter removed. questionId={}, remainingConnections={}",
                questionId,
                session.emitters.size()
        );
    }

    /**
     * 완료 또는 실패 후 보관 시간이 지난 스트림 상태를 메모리에서 제거한다.
     *
     * 1분마다 검사하며, 실제 제거 기준은 TERMINATED_SESSION_RETENTION_MILLIS다.
     */
    @Scheduled(fixedDelay = 60_000)
    public void cleanupTerminatedSessions() {
        Instant threshold = Instant.now().minusMillis(
                TERMINATED_SESSION_RETENTION_MILLIS
        );

        sessions.entrySet().removeIf(entry -> {
            StreamSession session = entry.getValue();

            synchronized (session) {
                return session.terminatedAt != null
                        && session.terminatedAt.isBefore(threshold);
            }
        });
    }

    /**
     * 질문 하나의 SSE 실행 상태다.
     *
     * DB에 저장되는 Entity가 아니라 현재 서버 프로세스 안에서만 유지되는
     * 일시적인 상태이므로 QaAnswerStreamService 내부 클래스로 둔다.
     */
    private static class StreamSession {

        // 현재 SSE 세션이 받아들일 AI 생성 시도 번호
        private int activeAttempt;

        // 현재까지 생성된 답변 전체
        private final StringBuilder content = new StringBuilder();

        // 현재 질문을 구독하고 있는 SSE 연결
        private final Set<SseEmitter> emitters =
                ConcurrentHashMap.newKeySet();

        // COMPLETED 또는 FAILED 이벤트
        private QaAnswerStreamEventDto terminalEvent;

        // 종료된 세션 정리 시간을 판단하기 위한 시각
        private Instant terminatedAt;
    }
}
