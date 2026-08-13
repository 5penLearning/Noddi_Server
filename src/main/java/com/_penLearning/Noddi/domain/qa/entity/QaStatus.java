package com._penLearning.Noddi.domain.qa.entity;

public enum QaStatus {
    PENDING,    // 질문 등록 후 AI 처리 대기
    PROCESSING, // AI 답변 생성 중
    ANSWERED,   // AI 답변 생성 완료
    FAILED      // AI 답변 생성 실패
}
