package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import com._penLearning.Noddi.global.exception.GeneralException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 팀 Q&A 피드에 등록된 독립 질문이다.
 * 질문 간 대화 문맥이나 꼬리질문은 연결하지 않으며, targetTeam이 RAG 검색 범위를 결정한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "QaQuestion")
public class QaQuestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long questionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questionerId", nullable = false)
    private User questioner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "targetTeamId", nullable = false)
    private Team targetTeam;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QaStatus status;

    @Column(nullable = false)
    private int generationAttempts;

    @Builder
    public QaQuestion(User questioner, Team targetTeam, String content) {
        this.questioner = questioner;
        this.targetTeam = targetTeam;
        this.content = content;
        this.status = QaStatus.PENDING;
        this.generationAttempts = 0;
    }

    public void startProcessing() {
        // 최초 처리 또는 이전 실패 작업의 재시도만 허용한다.
        if (status != QaStatus.PENDING && status != QaStatus.FAILED) {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }
        this.status = QaStatus.PROCESSING;
        this.generationAttempts++;
    }

    public void markAsAnswered() {
        validateProcessing();
        this.status = QaStatus.ANSWERED;
    }

    public void markAsFailed() {
        validateProcessing();
        this.status = QaStatus.FAILED;
    }

    public void markAsManualRequired() {
        if (status != QaStatus.PROCESSING && status != QaStatus.FAILED) {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }
        this.status = QaStatus.MANUAL_REQUIRED;
    }

    public void completeManualAnswer() {
        if (status != QaStatus.MANUAL_REQUIRED) {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }
        this.status = QaStatus.ANSWERED;
    }

    public boolean isCurrentAttempt(int attempt) {
        return generationAttempts == attempt;
    }

    public boolean canRetry(int maxAttempts) {
        return status != QaStatus.ANSWERED
                && status != QaStatus.MANUAL_REQUIRED
                && generationAttempts < maxAttempts;
    }

    private void validateProcessing() {
        // AI 생성이 실제로 시작된 질문만 완료/실패로 전환할 수 있다.
        if (status != QaStatus.PROCESSING) {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }
    }
}
