package com._penLearning.Noddi.domain.qa.event;

/** AI 자동 재시도가 모두 실패해 대상 팀의 직접 답변이 필요해졌음을 알린다. */
public record QaAiFinalFailureEvent(
        Long questionId,
        Long questionerId,
        Long targetTeamId,
        Long answerId,
        String noticeContent
) {
}
