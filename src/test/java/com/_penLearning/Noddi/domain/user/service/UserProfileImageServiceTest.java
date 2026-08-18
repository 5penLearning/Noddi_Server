package com._penLearning.Noddi.domain.user.service;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.domain.user.storage.ProfileImageStorage;
import com._penLearning.Noddi.domain.user.storage.StoredProfileImage;
import com._penLearning.Noddi.global.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileImageServiceTest {

    private static final String NEW_KEY = "11111111-1111-1111-1111-111111111111.png";
    private static final String OLD_KEY = "22222222-2222-2222-2222-222222222222.jpg";

    @Mock private UserRepository userRepository;
    @Mock private ProfileImageStorage profileImageStorage;
    @Mock private User user;

    @Test
    void replacesProfileImageAndDeletesPreviousFile() {
        UserProfileImageService service = new UserProfileImageService(userRepository, profileImageStorage);
        MockMultipartFile image = new MockMultipartFile("image", new byte[]{1});
        when(profileImageStorage.store(image)).thenReturn(new StoredProfileImage(NEW_KEY));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.updateProfileImage(NEW_KEY)).thenReturn(OLD_KEY);
        when(user.getUserId()).thenReturn(1L);
        when(user.getProfileImageKey()).thenReturn(NEW_KEY);

        var response = service.updateProfileImage(1L, image);

        assertThat(response.getProfileImageUrl())
                .isEqualTo("/api/v1/users/1/profile-image?v=" + NEW_KEY);
        verify(profileImageStorage).delete(OLD_KEY);
    }

    @Test
    void deletesNewFileWhenUserDoesNotExist() {
        UserProfileImageService service = new UserProfileImageService(userRepository, profileImageStorage);
        MockMultipartFile image = new MockMultipartFile("image", new byte[]{1});
        when(profileImageStorage.store(image)).thenReturn(new StoredProfileImage(NEW_KEY));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateProfileImage(999L, image))
                .isInstanceOf(GeneralException.class);

        verify(profileImageStorage).delete(NEW_KEY);
    }

    @Test
    void removesProfileImageKeyAndStoredFile() {
        UserProfileImageService service = new UserProfileImageService(userRepository, profileImageStorage);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(user.removeProfileImage()).thenReturn(OLD_KEY);

        service.deleteProfileImage(1L);

        verify(profileImageStorage).delete(OLD_KEY);
    }
}
