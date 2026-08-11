package com._penLearning.Noddi.domain.qa.dto;

import com._penLearning.Noddi.domain.qa.entity.AnswerType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class QaResponseDto {

    @Getter
    @Builder
    public static class CreateQuestion{
        private Long questionId;

        public static CreateQuestion from(Long questionId){
            return CreateQuestion.builder()
                    .questionId(questionId)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class CreateAnswer{
        private Long answerId;

        public static CreateAnswer from(Long answerId){
            return CreateAnswer.builder()
                    .answerId(answerId)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class QuestionInfo {
        private Long questionId;
        private Long targetTeamId;
        private String targetTeamName;
        private Long questionerId;
        private String questionerName;

        private String content;
        private QaStatus status;
        private LocalDateTime createdAt;

        public static QuestionInfo from(QaQuestion question) {
            return QuestionInfo.builder()
                    .questionId(question.getQuestionId())
                    .targetTeamId(question.getTargetTeam().getTeamId())
                    .targetTeamName(question.getTargetTeam().getName())
                    .questionerId(question.getQuestioner().getUserId())
                    .questionerName(question.getQuestioner().getName())
                    .content(question.getContent())
                    .status(question.getStatus())
                    .createdAt(question.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class QuestionDetail {
        private Long questionId;
        private String content;
        private QaStatus status;
        private String targetTeamName;
        private LocalDateTime createdAt;
        private Long questionerId;
        private String questionerName;
        private AnswerInfo answer; // 답변이 없으면 null 반환

        public static QuestionDetail of(QaQuestion question, QaAnswer answer) {
            return QuestionDetail.builder()
                    .questionId(question.getQuestionId())
                    .content(question.getContent())
                    .status(question.getStatus())
                    .targetTeamName(question.getTargetTeam().getName())
                    .createdAt(question.getCreatedAt())
                    .questionerId(question.getQuestioner().getUserId())
                    .questionerName(question.getQuestioner().getName())
                    .answer(answer != null ? AnswerInfo.from(answer) : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AnswerInfo {
        private Long answerId;
        private String content;
        private AnswerType answerType;
        private Long answeredById;
        private String answeredByName;

        public static AnswerInfo from(QaAnswer answer) {
            return AnswerInfo.builder()
                    .answerId(answer.getAnswerId())
                    .content(answer.getContent())
                    .answerType(answer.getAnswerType())
                    .answeredById(answer.getAnsweredBy() != null ? answer.getAnsweredBy().getUserId() : null)
                    .answeredByName(answer.getAnsweredBy() != null ? answer.getAnsweredBy().getName() : "AI")
                    .build();
        }
    }
}
