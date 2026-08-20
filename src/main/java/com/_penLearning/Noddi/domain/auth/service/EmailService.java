package com._penLearning.Noddi.domain.auth.service;

import com._penLearning.Noddi.domain.auth.code.AuthErrorCode;
import com._penLearning.Noddi.domain.auth.dto.AuthRequestDto;
import com._penLearning.Noddi.domain.organization.code.OrganizationErrorCode;
import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.util.EmailAsyncSender;
import com._penLearning.Noddi.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailService {
    private final RedisUtil redisUtil;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final EmailAsyncSender emailAsyncSender;

    private static final String CODE_PREFIX = "EMAIL_CODE:";
    private static final String VERIFIED_PREFIX = "EMAIL_VERIFIED:";
    private static final long CODE_EXPIRATION = 300L; // 이메일로 발송된 인증번호 유효 시간 (5분)
    private static final long VERIFIED_EXPIRATION = 900L; // 인증 성공 이메일에 대해 회원가입 완료까지 인증 상태 유지해 주는 시간 (15분)
    private static final long SEND_COOLDOWN = 60L; // 재발송 대기 시간 (1분)
    private static final int MAX_ATTEMPT = 5; // 최대 인증 시도 횟수 (5회)

    // 인증번호 발송
    public void sendVerificationCode(AuthRequestDto.EmailSendRequestDto request) {

        // 이미 가입 된 이메일에 전송 방지
        if(userRepository.existsByEmail(request.getEmail())){
            throw new GeneralException(AuthErrorCode.DUPLICATE_EMAIL);
        }

        // 요청된 조직이 존재하는지 확인하고, 입력한 이메일의 도메인이 해당 조직의 도메인과 일치하는지 검증
        Organization organization = organizationRepository.findById(request.getOrganizationId())
                .orElseThrow(() -> new GeneralException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND));

        validateEmailDomain(request.getEmail(), organization.getEmailDomain());

        // 발송 쿨다운 확인
        String cooldownKey = "EMAIL_COOLDOWN:" + request.getOrganizationId() + ":" + request.getEmail();
        if (redisUtil.hasKey(cooldownKey)) {
            throw new GeneralException(AuthErrorCode.EMAIL_SEND_COOLDOWN);
        }

        // 인증번호 생성 (6자리 난수)
        String authCode = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        // 메일 발송 (발송 중 에러 발생 시 여기서 예외가 터지므로 이래 Redis 로직은 실행되지 않음)
        emailAsyncSender.sendMailAsync(request.getEmail(), authCode);

        // 이메일 발송이 "성공적으로 끝난 경우에만" Redis에 상태를 저장
        String redisKey = CODE_PREFIX + request.getOrganizationId() + ":" + request.getEmail();
        String attemptKey = "EMAIL_ATTEMPT:" + request.getOrganizationId() + ":" + request.getEmail();

        redisUtil.setDataExpire(redisKey, authCode, CODE_EXPIRATION);
        redisUtil.setDataExpire(cooldownKey, "true", SEND_COOLDOWN);
        redisUtil.deleteData(attemptKey); // 기존 시도 횟수 초기화

    }

    // 인증번호 검증
    public void verifyCode(AuthRequestDto.EmailVerifyRequestDto request) {

        String codeKey = CODE_PREFIX + request.getOrganizationId() + ":" + request.getEmail();
        String attemptKey = "EMAIL_ATTEMPT:" + request.getOrganizationId() + ":" + request.getEmail();
        String verifiedKey = VERIFIED_PREFIX + request.getOrganizationId() + ":" + request.getEmail();

        String redisCode = redisUtil.getData(codeKey);

        // 인증 코드가 없거나 만료된 경우
        if (redisCode == null) {
            throw new GeneralException(AuthErrorCode.INVALID_VERIFICATION_CODE); // "인증번호가 일치하지 않거나 만료되었습니다."
        }

        // 인증 코드가 틀렸을 경우 (횟수 카운팅)
        if (!redisCode.equals(request.getCode())) {
            Long attempts = redisUtil.increment(attemptKey); // 시도 횟수 증가
            redisUtil.setDataExpire(attemptKey, String.valueOf(attempts), CODE_EXPIRATION); // 시도 횟수 키 5분 유지

            if (attempts >= MAX_ATTEMPT) {
                // 5회 이상 틀리면 즉시 코드 파기하여 브루트포스 차단
                redisUtil.deleteData(codeKey);
                redisUtil.deleteData(attemptKey);
                throw new GeneralException(AuthErrorCode.EXCEEDED_VERIFICATION_ATTEMPTS); // "인증 시도 횟수를 초과했습니다. 다시 발송해주세요."
            }

            // n회 틀렸다는 에러 (옵션: 메시지에 남은 횟수 전달 가능)
            throw new GeneralException(AuthErrorCode.INVALID_VERIFICATION_CODE);
        }

        // 검증 성공 시 기존 발송 코드 삭제 및 완료 상태 저장 (15분간 유지)
        redisUtil.deleteData(codeKey);
        redisUtil.setDataExpire(verifiedKey, "true", VERIFIED_EXPIRATION);
    }

    // 이메일 도메인 검증 메서드
    private void validateEmailDomain(String email, String allowedDomain) {
        String emailDomain = email.substring(email.indexOf("@") + 1);
        if (!emailDomain.equalsIgnoreCase(allowedDomain)) {
            throw new GeneralException(AuthErrorCode.INVALID_EMAIL_DOMAIN); // "해당 조직의 이메일 도메인과 일치하지 않습니다."
        }
    }
}
