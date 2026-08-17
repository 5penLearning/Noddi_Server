package com._penLearning.Noddi.domain.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class AuthResponseDto {

    // 회원가입 응답 DTO
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AuthSignupResponseDto { // 'class' 키워드 추가
        private Long userId;

        public static AuthSignupResponseDto from(Long userId) {
            return new AuthSignupResponseDto(userId);
        }
    }

    // 회원가입 화면에서 다른 회원들이 입력한 부서와 직함을 추천해주는 DTO
    @Getter
    @AllArgsConstructor
    public static class SignupProfileOptions {
        private List<String> departments;
        private List<String> positions;

        public static SignupProfileOptions of(
                List<String> departments,
                List<String> positions
        ) {
            return new SignupProfileOptions(
                    List.copyOf(departments),
                    List.copyOf(positions)
            );
        }
    }

    // 로그인 응답 DTO
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class AuthLoginResponseDto { // 'class' 키워드 추가
        private Long userId;
        private String accessToken; // Access Token 반환 필드 포함

        public static AuthLoginResponseDto of(Long userId, String accessToken) {
            return new AuthLoginResponseDto(userId, accessToken);
        }
    }
}
