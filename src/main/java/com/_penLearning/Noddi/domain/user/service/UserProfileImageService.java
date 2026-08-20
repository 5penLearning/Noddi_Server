package com._penLearning.Noddi.domain.user.service;

import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.dto.UserResponseDto;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.domain.user.storage.ProfileImageResource;
import com._penLearning.Noddi.domain.user.storage.ProfileImageStorage;
import com._penLearning.Noddi.domain.user.storage.StoredProfileImage;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserProfileImageService {

    private final UserRepository userRepository;
    private final ProfileImageStorage profileImageStorage;

    @Transactional
    public UserResponseDto.ProfileImageInfo updateProfileImage(Long userId, MultipartFile image) {
        StoredProfileImage storedImage = profileImageStorage.store(image);

        try {
            User user = getUserForProfileImageUpdateOrThrow(userId);
            String previousKey = user.updateProfileImage(storedImage.key());
            cleanupAfterTransaction(storedImage.key(), previousKey);
            return UserResponseDto.ProfileImageInfo.from(user);
        } catch (RuntimeException exception) {
            // DB 변경 전에 오류가 난 경우 방금 저장한 파일을 남기지 않는다.
            safeDelete(storedImage.key());
            throw exception;
        }
    }

    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = getUserForProfileImageUpdateOrThrow(userId);
        String previousKey = user.removeProfileImage();
        if (previousKey == null) {
            return;
        }

        deleteAfterCommit(previousKey);
    }

    public ProfileImageResource getProfileImage(Long userId) {
        User user = getUserOrThrow(userId);
        if (user.getProfileImageKey() == null) {
            throw new GeneralException(UserErrorCode.PROFILE_IMAGE_NOT_FOUND);
        }
        return profileImageStorage.load(user.getProfileImageKey());
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
    }

    private void cleanupAfterTransaction(String newKey, String previousKey) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(previousKey);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    safeDelete(previousKey);
                } else {
                    safeDelete(newKey);
                }
            }
        });
    }

    private void deleteAfterCommit(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            safeDelete(key);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                safeDelete(key);
            }
        });
    }

    private void safeDelete(String key) {
        if (key == null) {
            return;
        }

        try {
            profileImageStorage.delete(key);
        } catch (RuntimeException exception) {
            // DB 커밋은 완료됐으므로 파일 정리 실패를 사용자 요청 실패로 되돌리지 않는다.
            log.warn("Profile image cleanup failed. key={}", key, exception);
        }
    }

    private User getUserForProfileImageUpdateOrThrow(Long userId) {
        return userRepository.findByIdForProfileImageUpdate(userId)
                .orElseThrow(() ->
                        new GeneralException(UserErrorCode.USER_NOT_FOUND)
                );
    }
}
