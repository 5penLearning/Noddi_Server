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
        private String department;
        private String position;
        private String profileImageUrl;
        private Long organizationId;
        private String organizationName;

        public static ProfileInfo from(User user) {
            return ProfileInfo.builder()
                    .userId(user.getUserId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .department(user.getDepartment())
                    .position(user.getPosition())
                    .profileImageUrl(profileImageUrl(user))
                    .organizationId(user.getOrganization().getOrganizationId())
                    .organizationName(user.getOrganization().getName())
                    .build();
        }

        private static String profileImageUrl(User user) {
            if (user.getProfileImageKey() == null) {
                return null;
            }
            return "/api/v1/users/" + user.getUserId()
                    + "/profile-image?v=" + user.getProfileImageKey();
        }
    }

    @Getter
    @Builder
    public static class ProfileImageInfo {
        private String profileImageUrl;

        public static ProfileImageInfo from(User user) {
            return ProfileImageInfo.builder()
                    .profileImageUrl(
                            "/api/v1/users/" + user.getUserId()
                                    + "/profile-image?v=" + user.getProfileImageKey()
                    )
                    .build();
        }
    }
}
