package com._penLearning.Noddi.global.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. REST API + JWT를 사용할 것이므로 CSRF 방지 마크업 및 기본 폼 로그인, HttpBasic 차단
                .csrf(csrf -> csrf.disable())
                .formLogin(csrf -> csrf.disable())
                .httpBasic(csrf -> csrf.disable())

                // 2. 세션(Session) 대신 무상태(Stateless) JWT 토큰을 사용할 예정이므로 세션 끄기
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 3. URL 차단 및 개방 규칙
                .authorizeHttpRequests(auth -> auth
                                // Swagger 문서 접속 주소 허용
                                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**", "/v3/api-docs/**").permitAll()
                                // 회원가입, 로그인 API 허용
                                .requestMatchers("/api/auth/**").permitAll()
                                // 임시로 모든 API 테스트 위해 허용
                                .anyRequest().permitAll()

                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}