package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import com._penLearning.Noddi.global.exception.GeneralException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerType answerType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answeredById")
    private User answeredBy;

    @Builder
    public QaAnswer(QaQuestion question, String content, User answeredBy, AnswerType answerType) {
        validateAnswerer(answerType, answeredBy);
        this.question = question;
        this.content = content;
        this.answeredBy = answeredBy;
        this.answerType = answerType;
    }

    @PrePersist
    @PreUpdate
    private void validateAnswerer() {
        validateAnswerer(answerType, answeredBy);
    }

    private static void validateAnswerer(AnswerType answerType, User answeredBy) {
        if (answerType == null) {
            throw new GeneralException(QaErrorCode.INVALID_ANSWER_TYPE);
        }
        if (answerType == AnswerType.AI && answeredBy != null) {
            throw new GeneralException(QaErrorCode.INVALID_ANSWERER);
        }
        if (answerType == AnswerType.HUMAN && answeredBy == null) {
            throw new GeneralException(QaErrorCode.INVALID_ANSWERER);
        }
    }
}
