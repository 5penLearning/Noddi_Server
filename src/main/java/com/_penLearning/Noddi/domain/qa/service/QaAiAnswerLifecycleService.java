package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 생성 작업의 DB 상태만 관리하는 Q&A 명령 서비스다.
 * RAG 검색이나 OpenAI 호출은 맡지 않으며, 외부 호출 전후에 짧은 트랜잭션으로 사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QaAiAnswerLifecycleService {

    private final QaQuestionRepository qaQuestionRepository;
    private final QaAnswerRepository qaAnswerRepository;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;

    // 같은 질문에 답변을 두 번 생성하지 않기 위한 장치
    public boolean tryStart(Long questionId) {
        QaQuestion question = getQuestionWithLock(questionId);

        // 이벤트가 중복 전달되더라도 동일 질문의 AI 생성 작업은 하나만 실행한다.
        if (question.getStatus() == QaStatus.PROCESSING || question.getStatus() == QaStatus.ANSWERED) {
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

        // AI가 생성한 최초 답변을 저장한다. 이후 담당자 수정 시 현재 내용으로 교체될 수 있다.
        QaAnswer answer = qaAnswerRepository.save(QaAnswer.createAiAnswer(question, content));
        List<QaAnswerSource> answerSources = sources.stream()
                .map(source -> QaAnswerSource.builder()
                        .answer(answer)
                        .sourceType(source.sourceType())
                        .referenceId(source.sourceId())
                        .sourceTitle(source.sourceTitle())
                        .excerpt(source.content())
                        .build())
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

    private QaQuestion getQuestionWithLock(Long questionId) {
        return qaQuestionRepository.findByIdWithLock(questionId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));
    }
}
