package com._penLearning.Noddi.domain.project.entity;

public enum JoinStatus {
    INVITED,  // 초대를 받고 대기 중인 상태
    JOINED,   // 수락 완료 (정식 멤버)
    REJECTED  // 초대 거절
}