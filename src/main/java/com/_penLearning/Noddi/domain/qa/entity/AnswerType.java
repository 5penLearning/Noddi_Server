package com._penLearning.Noddi.domain.qa.entity;

public enum AnswerType {
    AI,     // AI가 생성한 답변
    SYSTEM, // AI 최종 실패 시 생성한 안내 답변
    TEAM    // 대상 팀원이 직접 작성한 답변
}
