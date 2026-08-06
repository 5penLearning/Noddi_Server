package com._penLearning.Noddi.domain.auth.dto;

public record TokenResponse(
        String grantType,
        String accessToken,
        String refreshToken
) {
}
