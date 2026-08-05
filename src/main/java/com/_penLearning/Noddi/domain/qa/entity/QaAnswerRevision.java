package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "QaAnswerRevision")
public class QaAnswerRevision extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long revisionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerId", nullable = false)
    private QaAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisedBy", nullable = false)
    private User revisedBy;

    @Lob
    @Column(nullable = false)
    private String content;

    private String revisionNote;

    @Builder
    public QaAnswerRevision(QaAnswer answer, User revisedBy, String content, String revisionNote) {
        this.answer = answer;
        this.revisedBy = revisedBy;
        this.content = content;
        this.revisionNote = revisionNote;
    }
}
