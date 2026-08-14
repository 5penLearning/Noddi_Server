package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 질문에 생성된 AI 답변이다.
 * 담당자가 수정하면 기존 내용을 보관하지 않고 현재 답변을 변경하며, 마지막 수정자만 기록한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "QaAnswer")
public class QaAnswer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionId", nullable = false, unique = true)
    private QaQuestion question;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerType answerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revisedBy")
    private User revisedBy;

    private QaAnswer(QaQuestion question, String content) {
        this.question = question;
        this.content = content;
        this.answerType = AnswerType.AI;
    }

    public static QaAnswer createAiAnswer(QaQuestion question, String content) {
        return new QaAnswer(question, content);
    }

    // 현재 답변을 수정하고 마지막 수정자를 기록한다
    public void revise(String content, User reviser) {
        this.content = content;
        this.revisedBy = reviser;
    }

    // 수정 여부 판단
    public boolean isRevised() {
        return revisedBy != null;
    }
}
