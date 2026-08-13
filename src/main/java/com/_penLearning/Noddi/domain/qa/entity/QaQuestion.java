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

    @Builder
    public QaQuestion(User questioner, Team targetTeam, String content) {
        this.questioner = questioner;
        this.targetTeam = targetTeam;
        this.content = content;
        this.status = QaStatus.PENDING;
    }

    public void startProcessing() {
        // 최초 처리 또는 이전 실패 작업의 재시도만 허용한다.
        if (status != QaStatus.PENDING && status != QaStatus.FAILED) {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }
        this.status = QaStatus.PROCESSING;
    }

    public void markAsAnswered() {
        validateProcessing();
        this.status = QaStatus.ANSWERED;
    }

    public void markAsFailed() {
        validateProcessing();
        this.status = QaStatus.FAILED;
    }

    private void validateProcessing() {
        // AI 생성이 실제로 시작된 질문만 완료/실패로 전환할 수 있다.
        if (status != QaStatus.PROCESSING) {
            throw new GeneralException(QaErrorCode.INVALID_QUESTION_STATUS);
        }
    }
}
