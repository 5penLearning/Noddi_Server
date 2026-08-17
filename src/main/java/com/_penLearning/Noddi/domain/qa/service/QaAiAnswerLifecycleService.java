package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.rag.generation.QaRagAnswerGenerator;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 생성 작업의 DB 상태만 관리하는 Q&A 명령 서비스다.
 * RAG 검색이나 OpenAI 호출은 맡지 않으며, 외부 호출 전후에 짧은 트랜잭션으로 사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QaAiAnswerLifecycleService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[근거\\s*(\\d+)]");

    private final QaQuestionRepository qaQuestionRepository;
    private final QaAnswerRepository qaAnswerRepository;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;
    private final QaAnswerRevisionRepository qaAnswerRevisionRepository;

    @Value("${qa.ai.max-generation-attempts:3}")
    private int maxGenerationAttempts = 3;

    // 같은 질문에 답변을 두 번 생성하지 않기 위한 장치
    public boolean tryStart(Long questionId) {
        QaQuestion question = getQuestionWithLock(questionId);

        // 이벤트가 중복 전달되더라도 동일 질문의 AI 생성 작업은 하나만 실행한다.
        if (question.getStatus() == QaStatus.PROCESSING || question.getStatus() == QaStatus.ANSWERED) {
            return false;
        }
        if (!question.canRetry(maxGenerationAttempts)) {
            return false;
        }

        question.startProcessing();
        return true;
    }

    // AI가 최종 답변을 생성한 후 호출하는 메서드
    // 최종 답변과 프롬프트에 제공한 근거를 하나의 트랜잭션으로 저장한다
    public Long complete(Long questionId, String content, List<RetrievedKnowledge> sources) {
        QaQuestion question = getQuestionWithLock(questionId);

        if (question.getStatus() != QaStatus.PROCESSING) {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }
        if (qaAnswerRepository.existsByQuestion(question)) {
            throw new GeneralException(QaErrorCode.ALREADY_ANSWERED);
        }

        List<Integer> citedSourceIndexes = citedSourceIndexes(content, sources.size());
        validateCitations(content, sources, citedSourceIndexes);

        // AI가 생성한 최초 답변을 저장한다. 이후 담당자 수정 시 현재 내용으로 교체될 수 있다.
        QaAnswer answer = qaAnswerRepository.save(QaAnswer.createAiAnswer(question, content));

        QaAnswerRevision initialRevision = QaAnswerRevision.createAiInitial(answer);
        qaAnswerRevisionRepository.save(initialRevision);

        List<QaAnswerSource> answerSources = citedSourceIndexes.stream()
                .map(citationIndex -> {
                    RetrievedKnowledge source = sources.get(citationIndex - 1);
                    return QaAnswerSource.builder()
                        .answer(answer)
                        .citationIndex(citationIndex)
                        .sourceType(source.sourceType())
                        .referenceId(source.sourceId())
                        .sourceTitle(source.sourceTitle())
                        .excerpt(source.content())
                        .build();
                })
                .toList();
        qaAnswerSourceRepository.saveAll(answerSources);

        question.markAsAnswered();
        return answer.getAnswerId();
    }

    // AI 호출 중 오류가 발생했을 때 호출된다
    public void fail(Long questionId) {
        QaQuestion question = getQuestionWithLock(questionId);

        // 완료 처리와 실패 처리가 경합한 경우 완료된 질문을 다시 FAILED로 바꾸지 않는다.
        if (question.getStatus() == QaStatus.PROCESSING) {
            question.markAsFailed();
        }
    }

    /** 오래 멈춘 작업을 재시도 가능한 상태로 바꾸고, 이벤트 재발행 여부를 반환한다. */
    public boolean prepareRecovery(Long questionId, LocalDateTime threshold) {
        QaQuestion question = getQuestionWithLock(questionId);

        if (question.getUpdatedAt().isAfter(threshold) || !question.canRetry(maxGenerationAttempts)) {
            return false;
        }
        if (question.getStatus() == QaStatus.PROCESSING) {
            question.markAsFailed();
        }
        return question.getStatus() == QaStatus.PENDING || question.getStatus() == QaStatus.FAILED;
    }

    private List<Integer> citedSourceIndexes(String content, int sourceCount) {
        Set<Integer> indexes = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(content);

        while (matcher.find()) {
            int citationIndex = Integer.parseInt(matcher.group(1));
            if (citationIndex >= 1 && citationIndex <= sourceCount) {
                indexes.add(citationIndex);
            }
        }
        return List.copyOf(indexes);
    }

    private void validateCitations(
            String content,
            List<RetrievedKnowledge> sources,
            List<Integer> citedSourceIndexes
    ) {
        boolean insufficientEvidenceAnswer = QaRagAnswerGenerator.INSUFFICIENT_EVIDENCE_MESSAGE
                .equals(content.strip());

        if (!sources.isEmpty() && !insufficientEvidenceAnswer && citedSourceIndexes.isEmpty()) {
            throw new GeneralException(QaErrorCode.INVALID_AI_ANSWER_CITATION);
        }
    }

    private QaQuestion getQuestionWithLock(Long questionId) {
        return qaQuestionRepository.findByIdWithLock(questionId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));
    }
}
