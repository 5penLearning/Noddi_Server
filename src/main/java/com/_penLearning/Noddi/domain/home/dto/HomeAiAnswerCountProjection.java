package com._penLearning.Noddi.domain.home.dto;

/**
 * 프로젝트별 미확인 AI 답변 개수 쿼리 결과를 받는 인터페이스 프로젝션이다.
 *
 * JPQL의 별칭(projectId, unreadCount)과 getter 이름이 대응한다.
 * 엔티티 전체가 필요하지 않은 집계 조회에서 필요한 값만 가져오기 위해 사용한다.
 */
public interface HomeAiAnswerCountProjection {

    Long getProjectId();

    long getUnreadCount();
}
