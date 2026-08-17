package com._penLearning.Noddi.domain.qa.dto;

import com._penLearning.Noddi.domain.qa.entity.AnswerType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.RevisionEditorType;
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
        private boolean canAnswer;

        public static QuestionDetail of(
                QaQuestion question,
                QaAnswer answer,
                List<QaAnswerSource> sources,
                boolean canAnswer
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
                    .canAnswer(canAnswer)
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
        private boolean canAnswer;

        public static FeedItem of(
                QaQuestion question,
                QaAnswer answer,
                List<QaAnswerSource> sources,
                boolean canAnswer
        ) {
            return FeedItem.builder()
                    .question(FeedQuestion.from(question))
                    .status(question.getStatus())
                    .answer(
                            answer != null ? FeedAnswer.from(answer, sources) : null
                    )
                    .canAnswer(canAnswer)
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

    @Getter
    @Builder
    public static class AnswerRevisionHistory {

        private Long answerId;

        // 프론트가 "2/3"의 전체 개수를 표시할 때 사용한다.
        private int totalVersions;

        /*
         * 현재 사용자가 AI 최초 답변의 출처를 볼 수 있는지 나타낸다.
         *
         * 질문 대상 팀 구성원만 true이고,
         * 같은 프로젝트의 다른 팀 구성원에게는 false를 반환한다.
         */
        private boolean canViewSources;

        // versionNumber 오름차순으로 반환한다.
        private List<AnswerRevisionItem> revisions;

        public static AnswerRevisionHistory of(
                QaAnswer answer,
                List<QaAnswerRevision> revisions,
                List<QaAnswerSource> initialSources,
                boolean canViewSources
        ) {
            List<AnswerRevisionItem> revisionItems = revisions.stream()
                    .map(revision -> {
                        /*
                         * 출처는 AI 최초 답변인 version 1에만 연결한다.
                         *
                         * 담당자 수정본은 기존 AI 출처가 수정 내용을
                         * 뒷받침한다고 보장할 수 없으므로 빈 목록을 반환한다.
                         */
                        boolean shouldExposeSources = canViewSources && revision.getVersionNumber() == 1;

                        List<QaAnswerSource> visibleSources =
                                shouldExposeSources
                                        ? initialSources
                                        : List.of();

                        return AnswerRevisionItem.of(
                                revision,
                                visibleSources
                        );
                    })
                    .toList();

            return AnswerRevisionHistory.builder()
                    .answerId(answer.getAnswerId())
                    .totalVersions(revisionItems.size())
                    .canViewSources(canViewSources)
                    .revisions(revisionItems)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class AnswerRevisionItem {

        private int versionNumber;
        private String content;

        // AI 최초 답변인지, 담당자 수정본인지 구분한다.
        private RevisionEditorType editorType;

        /*
         * AI는 User 엔티티가 아니므로:
         * editorId   → null
         * editorName → "AI"
         *
         * HUMAN은 실제 수정자 정보를 반환한다.
         */
        private Long editorId;
        private String editorName;

        /*
         * 해당 버전이 생성된 시간이다.
         *
         * version 1에서는 AI 답변 생성 시간,
         * version 2 이상에서는 담당자 수정 시간이 된다.
         */
        private LocalDateTime createdAt;

        // 대상 팀원이 조회한 AI 최초 버전에만 값이 들어간다.
        private List<AnswerSourceInfo> sources;

        public static AnswerRevisionItem of(
                QaAnswerRevision revision,
                List<QaAnswerSource> sources
        ) {
            boolean editedByHuman =
                    revision.getEditorType() == RevisionEditorType.HUMAN;

            return AnswerRevisionItem.builder()
                    .versionNumber(revision.getVersionNumber())
                    .content(revision.getContent())
                    .editorType(revision.getEditorType())
                    .editorId(
                            editedByHuman
                                    ? revision.getRevisedBy().getUserId()
                                    : null
                    )
                    .editorName(
                            editedByHuman
                                    ? revision.getRevisedBy().getName()
                                    : "AI"
                    )
                    .createdAt(revision.getCreatedAt())
                    .sources(
                            sources.stream()
                                    .map(AnswerSourceInfo::from)
                                    .toList()
                    )
                    .build();
        }
    }
}
