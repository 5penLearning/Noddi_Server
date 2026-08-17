package com._penLearning.Noddi.domain.qa.dto;

import com._penLearning.Noddi.domain.qa.entity.AnswerType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.team.entity.Team;
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
        private LocalDateTime lastRevisedAt;

        public static AnswerInfo from(QaAnswer answer) {
            return AnswerInfo.builder()
                    .answerId(answer.getAnswerId())
                    .content(answer.getContent())
                    .answerType(answer.getAnswerType())
                    .revised(answer.isRevised())
                    .lastRevisedById(answer.isRevised() ? answer.getRevisedBy().getUserId() : null)
                    .lastRevisedByName(answer.isRevised() ? answer.getRevisedBy().getName() : null)
                    .lastRevisedAt(answer.isRevised() ? answer.getUpdatedAt() : null)
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

    @Getter
    @Builder
    public static class Feed {
        private Long projectId;
        private String projectName;

        private Long teamId;
        private String teamName;

        /*
         * 현재 사용자가 이 팀의 구성원인지 나타낸다.
         *
         * true인 경우에만 AI 답변의 회의록·팀 페이지 출처를 제공한다.
         * 프론트는 이 값과 sources를 기준으로 참고 자료 버튼을 표시한다.
         */
        private boolean canViewSources;

        private List<FeedItem> items;

        private Long nextCursor;
        private boolean hasNext;

        public static Feed of(Team team, List<FeedItem> items, Long nextCursor, boolean hasNext, boolean canViewSources) {
            return Feed.builder()
                    .projectId(team.getProject().getProjectId())
                    .projectName(team.getProject().getName())
                    .teamId(team.getTeamId())
                    .teamName(team.getName())
                    .canViewSources(canViewSources)
                    .items(items)
                    .nextCursor(nextCursor)
                    .hasNext(hasNext)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FeedItem {
        private FeedQuestion question;
        private QaStatus status;
        private FeedAnswer answer;

        public static FeedItem of(QaQuestion question, QaAnswer answer, List<QaAnswerSource> sources) {
            return FeedItem.builder()
                    .question(FeedQuestion.from(question))
                    .status(question.getStatus())
                    .answer(
                            answer != null ? FeedAnswer.from(answer, sources) : null
                    )
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FeedQuestion {
        private Long questionId;

        private Long questionerId;
        private String questionerName;

        private String content;
        private LocalDateTime createdAt;
        //현재 소속 부서 + 직함 엔티티가 없어서 일단 빼고 이름만 반환

        public static FeedQuestion from(QaQuestion question){
            return FeedQuestion.builder()
                    .questionId(question.getQuestionId())
                    .questionerId(question.getQuestioner().getUserId())
                    .questionerName(question.getQuestioner().getName())
                    .content(question.getContent())
                    .createdAt(question.getCreatedAt())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class FeedAnswer {
        private Long answerId;
        private String content;
        private AnswerType answerType;

        private boolean revised;

        private Long lastRevisedById;
        private String lastRevisedByName;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        private List<AnswerSourceInfo> sources;

        public static FeedAnswer from(QaAnswer answer, List<QaAnswerSource> sources) {
            return FeedAnswer.builder()
                    .answerId(answer.getAnswerId())
                    .content(answer.getContent())
                    .answerType(answer.getAnswerType())
                    .revised(answer.isRevised())
                    .lastRevisedById(
                            answer.isRevised() ? answer.getRevisedBy().getUserId() : null
                    )
                    .lastRevisedByName(
                            answer.isRevised() ? answer.getRevisedBy().getName() : null
                    )
                    .createdAt(answer.getCreatedAt())
                    .updatedAt(answer.getUpdatedAt())
                    .sources(sources.stream().map(AnswerSourceInfo::from).toList())
                    .build();
        }
    }
}
