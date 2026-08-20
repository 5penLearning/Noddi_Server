package com._penLearning.Noddi.domain.notification.dto;

public enum NotificationNavigationType {
    // 특정 질문이 표시된 Q&A 피드
    QA_QUESTION,

    // 여러 AI 검토 알림을 묶은 대상 팀 Q&A 피드
    QA_TEAM_FEED,

    // 마이페이지의 받은 초대장 화면
    MY_INVITATIONS,

    //홈 화면의 프로젝트별 To-do list
    HOME_ACTION_ITEMS
}
