package com._penLearning.Noddi.domain.qa.dto;

import com._penLearning.Noddi.domain.qa.entity.AnswerType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class QaResponseDto {

    @Getter
    @Builder
    public static class CreateQuestion {
        private Long questionId;
        private QaStatus status;

        public static CreateQuestion from(QaQuestion question) {
            return CreateQuestion.builder()
                    .questionId(question.getQuestionId())
                    .status(question.getStatus())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ReviseAnswer {
        private Long answerId;

        public static ReviseAnswer from(QaAnswer answer) {
            return ReviseAnswer.builder()
                    .answerId(answer.getAnswerId())
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
        private AnswerInfo answer;
        private List<AnswerSourceInfo> sources;

        public static QuestionDetail of(
                QaQuestion question,
                QaAnswer answer,
                List<QaAnswerSource> sources
        ) {
            return QuestionDetail.builder()
                    .questionId(question.getQuestionId())
                    .content(question.getContent())
                    .status(question.getStatus())
                    .targetTeamName(question.getTargetTeam().getName())
                    .createdAt(question.getCreatedAt())
                    .questionerId(question.getQuestioner().getUserId())
                    .questionerName(question.getQuestioner().getName())
                    .answer(answer != null ? AnswerInfo.from(answer) : null)
                    .sources(sources.stream().map(AnswerSourceInfo::from).toList())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AnswerInfo {
        private Long answerId;
        // 수정 전 원문은 노출하지 않고 현재 최종 답변만 반환한다.
        private String content;
        private AnswerType answerType;
        private boolean revised;
        private Long lastRevisedById;
        private String lastRevisedByName;

        public static AnswerInfo from(QaAnswer answer) {
            return AnswerInfo.builder()
                    .answerId(answer.getAnswerId())
                    .content(answer.getContent())
                    .answerType(answer.getAnswerType())
                    .revised(answer.isRevised())
                    .lastRevisedById(answer.isRevised() ? answer.getRevisedBy().getUserId() : null)
                    .lastRevisedByName(answer.isRevised() ? answer.getRevisedBy().getName() : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AnswerSourceInfo {
        private int citationIndex;
        private SourceType sourceType;
        private Long referenceId;
        private String sourceTitle;
        private String excerpt;

        public static AnswerSourceInfo from(QaAnswerSource source) {
            return AnswerSourceInfo.builder()
                    .citationIndex(source.getCitationIndex())
                    .sourceType(source.getSourceType())
                    .referenceId(source.getReferenceId())
                    .sourceTitle(source.getSourceTitle())
                    .excerpt(source.getExcerpt())
                    .build();
        }
    }
}
