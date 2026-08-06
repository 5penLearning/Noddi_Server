package com._penLearning.Noddi.domain.auth.dto;

public record TokenResponseDto(
        String grantType,
        String accessToken,
        String refreshToken
) {
}
