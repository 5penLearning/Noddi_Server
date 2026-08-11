package com._penLearning.Noddi.domain.user.service;


import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.dto.UserRequestDto;
import com._penLearning.Noddi.domain.user.dto.UserResponseDto;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

     // 내 프로필 및 조직 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDto.ProfileInfo getMyProfile(Long userId) {
        User user = userRepository.findByIdWithOrganization(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        return UserResponseDto.ProfileInfo.from(user);
    }

    // 내 프로필 수정
    @Transactional
    public void updateProfile(Long userId, String newName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        user.updateProfile(newName);
    }

    // 비번 변경
    @Transactional
    public void updatePassword(Long userId, UserRequestDto.UpdatePassword request) {
        User user = userRepository.findById(userId)
                // 실제 적용 시 UserErrorCode.USER_NOT_FOUND 사용 권장
                .orElseThrow(() -> new RuntimeException("유저를 찾을 수 없습니다."));

        // 1. 현재 비밀번호 일치 여부 검증
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            // 실제 적용 시 UserErrorCode.INVALID_PASSWORD 등 커스텀 예외 사용 권장
            throw new RuntimeException("현재 비밀번호가 일치하지 않습니다.");
        }

        // 2. 새 비밀번호와 현재 비밀번호가 같은지 방어 로직 (선택 사항)
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("새로운 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        // 3. 비밀번호 암호화 후 엔티티 업데이트
        user.updatePassword(passwordEncoder.encode(request.getNewPassword()));
    }
}
