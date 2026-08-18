package com._penLearning.Noddi.domain.home.dto;

/** 현재 사용자가 참여 중인 프로젝트 탭을 구성하기 위한 조회 프로젝션이다. */
public interface HomeProjectProjection {

    Long getProjectId();

    String getProjectName();
}
