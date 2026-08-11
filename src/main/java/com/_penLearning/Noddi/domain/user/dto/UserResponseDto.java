package com._penLearning.Noddi.domain.user.dto;

import com._penLearning.Noddi.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

public class UserResponseDto {

    @Getter
    @Builder
    public static class ProfileInfo {
        private Long userId;
        private String email;
        private String name;
        private Long organizationId;
        private String organizationName;

        public static ProfileInfo from(User user) {
            return ProfileInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .organizationId(user.getOrganization().getOrganizationId())
                    .organizationName(user.getOrganization().getName())
                    .build();
        }
    }
}
