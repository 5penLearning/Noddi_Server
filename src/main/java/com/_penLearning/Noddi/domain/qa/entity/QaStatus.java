package com._penLearning.Noddi.domain.qa.entity;

public enum QaStatus {
    PENDING,    // 질문 등록 후 AI 처리 대기
    PROCESSING, // AI 답변 생성 중
    ANSWERED,   // AI 답변 생성 완료
    FAILED,     // AI 답변 생성 실패, 자동 재시도 가능
    MANUAL_REQUIRED // AI 최종 실패, 대상 팀의 직접 답변 대기
}
