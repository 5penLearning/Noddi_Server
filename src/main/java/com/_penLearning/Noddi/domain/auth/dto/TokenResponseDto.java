package com._penLearning.Noddi.domain.auth.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TokenResponseDto {
    private String grantType;
    private String accessToken;

    public TokenResponseDto(String bearer, String accessToken) {
        this.grantType = bearer;
        this.accessToken = accessToken;
    }
}
