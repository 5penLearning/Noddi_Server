package com._penLearning.Noddi.domain.auth.service;

import com._penLearning.Noddi.domain.auth.code.AuthErrorCode;
import com._penLearning.Noddi.domain.auth.dto.LoginRequest;
import com._penLearning.Noddi.domain.auth.dto.SignupRequest;
import com._penLearning.Noddi.domain.auth.dto.TokenResponse;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final OrganizationRepository organizationRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public Long signup(SignupRequest request){
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new GeneralException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        Organization organization = organizationRepository.findById(request.organizationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다."));//OrganizationErrorCode 작성 시 교체 예정

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.password());

        // 3. User 엔티티 생성 및 저장
        User user = User.builder()
                .email(request.email())
                .password(encodedPassword)
                .name(request.name())
                .build();

        return userRepository.save(user).getUserId();
    }

    public TokenResponse login(LoginRequest request) {
        // 1. 이메일 존재 여부 확인
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 일치 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new GeneralException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 3. JWT 토큰 생성 및 반환
        return jwtProvider.generateToken(user.getUserId(), user.getEmail());
    }
}
