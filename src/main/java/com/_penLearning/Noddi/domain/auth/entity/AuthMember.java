package com._penLearning.Noddi.domain.auth.entity;

import lombok.Builder;
import lombok.Getter;

@Getter
public class AuthMember {

    private final Long userId;
    private final String email;
    // 필요 시 추가 (예: private final Long organizationId; private final String role;)

    @Builder
    public AuthMember(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }
}
