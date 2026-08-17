package com._penLearning.Noddi.domain.qa.event;

/**
 * 사용자에게 노출할 답변이 등록되거나 변경됐음을 알리는 도메인 이벤트다.
 * 알림 도메인은 publishType에 따라 수신자와 문구를 결정한다.
 * actorId는 답변을 작성 또는 수정한 사용자이며 AI가 생성한 경우 null이다.
 */
public record QaAnswerPublishedEvent(
        Long questionId,
        Long answerId,
        Long questionerId,
        Long targetTeamId,
        Long actorId,
        QaAnswerPublishType publishType
) {
}
