package com._penLearning.Noddi.global.security;

import com._penLearning.Noddi.domain.auth.dto.TokenResponseDto;
import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtProvider {
    private final SecretKey key;
    private final long accessTokenExpiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration
    ) {

        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpiration = accessTokenExpiration;
    }

    // Access Token 발급 (userId, email 저장)
    public TokenResponseDto generateToken(Long userId, String email) {
        Date now = new Date();
        Date accessTokenExpiresIn = new Date(now.getTime() + accessTokenExpiration);

        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(accessTokenExpiresIn)
                .signWith(key)
                .compact();

        return new TokenResponseDto("Bearer", accessToken);
    }

    // 토큰에서 userId(Subject) 추출
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // 토큰 유효성 검증
    public boolean validateToken(String token) {
        Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token);
        return true;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    //토큰에서 Claims를 추출하여 AuthMember 객체로 변환
    public AuthMember getAuthMember(String token) {
        Claims claims = parseClaims(token);

        // Subject에 저장된 userId 추출 (String -> Long 변환)
        Long userId = Long.parseLong(claims.getSubject());

        // Custom Claim에서 email 추출
        String email = claims.get("email", String.class);

        return AuthMember.builder()
                .userId(userId)
                .email(email)
                .build();
    }
}
