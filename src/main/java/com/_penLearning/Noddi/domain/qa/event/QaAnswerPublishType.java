package com._penLearning.Noddi.domain.qa.event;

/** 사용자에게 노출되는 답변이 어떤 경로로 등록 또는 변경됐는지 나타낸다. */
public enum QaAnswerPublishType {
    AI_GENERATED,
    TEAM_PROVIDED,
    ANSWER_REVISED
}
