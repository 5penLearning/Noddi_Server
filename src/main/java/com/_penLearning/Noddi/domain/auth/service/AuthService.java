package com._penLearning.Noddi.domain.auth.service;

import com._penLearning.Noddi.domain.auth.code.AuthErrorCode;
import com._penLearning.Noddi.domain.auth.dto.AuthRequestDto;
import com._penLearning.Noddi.domain.auth.dto.AuthResponseDto;
import com._penLearning.Noddi.domain.auth.dto.TokenResponseDto;
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
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final OrganizationRepository organizationRepository;
    private final JwtProvider jwtProvider;

    @Transactional
    public AuthResponseDto.AuthSignupResponseDto signup(AuthRequestDto.SignupRequestDto request){
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GeneralException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 조직입니다."));//OrganizationErrorCode 작성 시 교체 예정

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 3. User 엔티티 생성 및 저장
        User user = User.builder()
                .organization(organization)
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .build();

        // 4. User 엔티티 생성 및 저장
        User savedUser = userRepository.save(user);

        // 5. 생성된 userId 기반 DTO 반환
        return AuthResponseDto.AuthSignupResponseDto.from(savedUser.getUserId());
    }

    public AuthResponseDto.AuthLoginResponseDto login(AuthRequestDto.LoginRequestDto request) {
        // 1. 이메일 존재 여부 확인
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_CREDENTIALS));

        // 2. 비밀번호 일치 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new GeneralException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // 3. JWT 토큰 생성
        TokenResponseDto tokenResponse = jwtProvider.generateToken(user.getUserId(), user.getEmail());

        // 4. AuthLoginResponseDto 형태로 변환하여 반환
        return AuthResponseDto.AuthLoginResponseDto.of(user.getUserId(), tokenResponse.getAccessToken());
    }
}
