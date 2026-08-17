package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 하나의 답변이 변경된 버전 이력을 저장한다.
 *
 * QaAnswer에는 피드에서 보여줄 최신 답변을 저장하고,
 * QaAnswerRevision에는 AI 원문부터 모든 수정본을 누적 저장한다.
 *
 * 수정 이력은 생성된 이후 내용을 변경하지 않는 불변 데이터로 사용한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "QaAnswerRevision",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_qa_answer_revision_version",
                columnNames = {"answerId", "versionNumber"}
        )
)
public class QaAnswerRevision extends BaseEntity {

    private static final int AI_INITIAL_VERSION = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long revisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerId", nullable = false)
    private QaAnswer answer;

    @Column(nullable = false)
    private int versionNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevisionEditorType editorType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisedById")
    private User revisedBy;

    //빌더패턴 적용하지 않음 - 필드 설정을 보장해주지 않기때문
    private QaAnswerRevision(
            QaAnswer answer,
            int versionNumber,
            String content,
            RevisionEditorType editorType,
            User revisedBy
    ) {
        this.answer = answer;
        this.versionNumber = versionNumber;
        this.content = content;
        this.editorType = editorType;
        this.revisedBy = revisedBy;
    }

    /**
     * AI가 생성한 최초 답변을 버전 1로 만든다.
     *
     * AI는 User 엔티티가 아니므로 revisedBy를 null로 저장한다.
     * 조회 DTO에서 editorType이 AI이면 수정자 이름을 "AI"로 변환한다.
     */
    public static QaAnswerRevision createAiInitial(
            QaAnswer answer
    ) {
        return new QaAnswerRevision(
                answer,
                AI_INITIAL_VERSION,
                answer.getContent(),
                RevisionEditorType.AI,
                null
        );
    }

    /**
     * 대상 팀 담당자가 수정한 버전을 만든다.
     *
     * versionNumber는 서비스에서 현재 마지막 버전을 조회한 뒤
     * 1을 더해서 전달한다.
     */
    public static QaAnswerRevision createHumanRevision(
            QaAnswer answer,
            int versionNumber,
            String content,
            User reviser
    ) {
        return new QaAnswerRevision(
                answer,
                versionNumber,
                content,
                RevisionEditorType.HUMAN,
                reviser
        );
    }
}
