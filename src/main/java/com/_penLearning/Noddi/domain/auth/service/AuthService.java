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
import com._penLearning.Noddi.global.util.RedisUtil;
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
    private final RedisUtil redisUtil;

    @Transactional
    public AuthResponseDto.AuthSignupResponseDto signup(AuthRequestDto.SignupRequestDto request) {
        // 1. 이메일 중복 검사
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new GeneralException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 2. 이메일 최종 인증 여부 검증 (Redis 확인)
        String verifiedKey = "EMAIL_VERIFIED:" + request.getOrganizationId() + ":" + request.getEmail();
        String isVerified = redisUtil.getData(verifiedKey);

        if (!"true".equals(isVerified)) {
            throw new GeneralException(AuthErrorCode.UNVERIFIED_EMAIL); // "이메일 인증이 완료되지 않았습니다."
        }

        // 3. 조직 조회
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new IllegalArgumentException("조직이 존재하지 않습니다.")); //OrganizationErrorCode 작성 시 교체 예정

        // 4. 비밀번호 암호화 및 User 생성
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .email(request.getEmail())
                .password(encodedPassword)
                .name(request.getName())
                .organization(organization)
                .build();

        User savedUser = userRepository.save(user);

        // 5. 회원가입 완료 후 이메일 인증 상태 키 삭제 (재사용 방지)
        redisUtil.deleteData(verifiedKey);

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
