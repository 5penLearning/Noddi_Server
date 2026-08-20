package com._penLearning.Noddi.domain.notification.entity;

public enum NotificationType {

    // 질문자에게 AI 또는 팀원 답변이 등록됐음을 알림
    QA_ANSWERED,

    // 질문자에게 담당 팀의 직접 답변을 기다리고 있다고 안내
    QA_ANSWER_WAITING,

    // 질문자에게 기존 답변이 수정됐음을 알림
    QA_ANSWER_REVISED,

    // 대상 팀원에게 AI가 대신 답변했으니 검토하라고 알림
    QA_AI_REVIEW_REQUIRED,

    // 대상 팀원에게 AI 최종 실패로 직접 답변이 필요함을 알림
    QA_AI_FAILED,

    TEAM_INVITE,
    PROJECT_INVITE,

    // 아직 공통 알림함과 연동하지 않지만 기존 값은 유지
    ACTION_ITEM_ASSIGNED,
    MENTION
}
