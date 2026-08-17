package com._penLearning.Noddi.domain.qa.dto;

import com._penLearning.Noddi.domain.qa.entity.QaStatus;

/**
 * 내부 AI 처리 상태를 사용자 권한에 맞게 변환한 Q&A 응답 상태다.
 * 대상 팀원이 아닌 사용자에게는 재시도 실패나 수동 답변 필요 여부를 직접 노출하지 않는다.
 */
public enum QaResponseStatus {
    PENDING,
    PROCESSING,
    ANSWERED,
    FAILED,
    MANUAL_REQUIRED,
    TEAM_ANSWER_PENDING;

    public static QaResponseStatus from(QaStatus status, boolean canViewFailureStatus) {
        if (!canViewFailureStatus && status == QaStatus.FAILED) {
            return PROCESSING;
        }
        if (!canViewFailureStatus && status == QaStatus.MANUAL_REQUIRED) {
            return TEAM_ANSWER_PENDING;
        }
        return valueOf(status.name());
    }
}
