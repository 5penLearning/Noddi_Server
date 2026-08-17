package com._penLearning.Noddi.domain.user.service;

import com._penLearning.Noddi.domain.user.dto.UserRequestDto;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private User user;
    @Mock private UserRequestDto.UpdateProfile request;

    @Test
    void updatesNameDepartmentAndPosition() {
        UserService service = new UserService(userRepository, passwordEncoder);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(request.getName()).thenReturn("김노디");
        when(request.getDepartment()).thenReturn("플랫폼팀");
        when(request.getPosition()).thenReturn("백엔드 개발자");

        service.updateProfile(1L, request);

        verify(user).updateProfile("김노디", "플랫폼팀", "백엔드 개발자");
    }
}
