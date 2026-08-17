package com._penLearning.Noddi.domain.qa.dto;

import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Q&A AI 답변 생성 과정에서 SSE로 전달하는 응답 DTO다.
 */
public record QaAnswerStreamEventDto(
        EventType type,
        Long questionId,
        Long answerId,
        QaStatus status,
        String content,
        String delta,
        Integer attempt
) {

    /**
     * SSE 연결이 정상적으로 생성됐음을 알리는 이벤트
     *
     * 사용자가 답변 생성 도중 또는 완료 이후에 접속할 수 있으므로
     * 현재 질문 상태를 외부에서 전달받는다.
     */
    public static QaAnswerStreamEventDto connected(
            Long questionId,
            QaStatus status
    ) {
        return new QaAnswerStreamEventDto(
                EventType.CONNECTED,
                questionId,
                null,
                status,
                null,
                null,
                null
        );
    }

    /**
     * 현재까지 생성된 답변 전체를 전달한다.
     *
     * 사용자가 AI 답변 생성 도중 늦게 접속하거나 SSE 연결이 끊어진 뒤
     * 다시 접속했을 때 이전 답변 조각을 복구하는 용도로 사용한다.
     *
     * 프론트에서는 기존 문자열에 추가하지 않고 content로 교체해야 한다.
     */
    public static QaAnswerStreamEventDto snapshot(
            Long questionId,
            String content
    ) {
        return new QaAnswerStreamEventDto(
                EventType.SNAPSHOT,
                questionId,
                null,
                QaStatus.PROCESSING,
                content,
                null,
                null
        );
    }

    /**
     * AI가 새롭게 생성한 답변 조각 하나를 전달한다.
     *
     * 프론트에서는 delta 값을 현재까지 표시한 답변 뒤에 이어 붙인다.
     */
    public static QaAnswerStreamEventDto chunk(
            Long questionId,
            String delta
    ) {
        return new QaAnswerStreamEventDto(
                EventType.CHUNK,
                questionId,
                null,
                QaStatus.PROCESSING,
                null,
                delta,
                null
        );
    }

    /**
     * AI 답변 전체가 DB에 저장된 후 전달한다.
     *
     * content에는 최종 답변 전체를 넣는다.
     * 스트리밍 과정에서 일부 chunk를 놓쳤더라도 프론트가 마지막에
     * content로 화면을 교체하면 DB의 최종 답변과 동일해진다.
     */
    public static QaAnswerStreamEventDto completed(
            Long questionId,
            Long answerId,
            String content
    ) {
        return new QaAnswerStreamEventDto(
                EventType.COMPLETED,
                questionId,
                answerId,
                QaStatus.ANSWERED,
                content,
                null,
                null
        );
    }

    /**
     * AI 답변 생성 또는 저장에 실패했음을 알린다.
     *
     * 프론트는 답변 생성 로딩을 종료하고 실패 또는 재시도 UI를 표시한다.
     */
    public static QaAnswerStreamEventDto failed(Long questionId) {
        return new QaAnswerStreamEventDto(
                EventType.FAILED,
                questionId,
                null,
                QaStatus.FAILED,
                null,
                null,
                null
        );
    }

    public static QaAnswerStreamEventDto retrying(Long questionId, int attempt) {
        return new QaAnswerStreamEventDto(
                EventType.RETRYING,
                questionId,
                null,
                QaStatus.FAILED,
                null,
                null,
                attempt
        );
    }

    public static QaAnswerStreamEventDto manualRequired(
            Long questionId,
            Long answerId,
            String content
    ) {
        return new QaAnswerStreamEventDto(
                EventType.MANUAL_REQUIRED,
                questionId,
                answerId,
                QaStatus.MANUAL_REQUIRED,
                content,
                null,
                null
        );
    }

    /**
     * SSE로 전달하는 이벤트 종류다.
     *
     * 질문 자체의 DB 상태인 QaStatus와 달리,
     * 클라이언트가 어떤 동작을 해야 하는지 구분하기 위한 통신 규격이다.
     */
    public enum EventType {

        // SSE 연결 성공
        CONNECTED("connected"),

        // 현재까지 생성된 전체 답변 전달
        SNAPSHOT("snapshot"),

        // 새롭게 생성된 답변 조각 전달
        CHUNK("chunk"),

        // 최종 답변 저장 완료
        COMPLETED("completed"),

        // AI 생성 실패 후 자동 재시도 대기
        RETRYING("retrying"),

        // 자동 재시도 최종 실패 후 대상 팀 답변 대기
        MANUAL_REQUIRED("manual_required"),

        // 답변 생성 실패
        FAILED("failed");

        private final String eventName;

        EventType(String eventName) {
            this.eventName = eventName;
        }

        /**
         * SSE의 event 이름과 JSON의 type 값에 사용한다.
         *
         * @JsonValue를 사용했기 때문에 JSON에는 "CHUNK"가 아니라
         * 프론트에서 사용하기 편한 "chunk"로 직렬화된다.
         */
        @JsonValue
        public String getEventName() {
            return eventName;
        }
    }
}
