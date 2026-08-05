package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "QaAnswerFeedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"answerId", "userId"})
})
public class QaAnswerFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerId", nullable = false)
    private QaAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeedbackRating rating;

    private String reason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public QaAnswerFeedback(QaAnswer answer, User user, FeedbackRating rating, String reason) {
        this.answer = answer;
        this.user = user;
        this.rating = rating;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
