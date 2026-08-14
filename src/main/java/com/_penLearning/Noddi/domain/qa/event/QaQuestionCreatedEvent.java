package com._penLearning.Noddi.domain.qa.event;

/** 질문 저장 커밋 이후 AI 답변 생성을 시작하기 위한 도메인 이벤트다. */
public record QaQuestionCreatedEvent(Long questionId) {
}
