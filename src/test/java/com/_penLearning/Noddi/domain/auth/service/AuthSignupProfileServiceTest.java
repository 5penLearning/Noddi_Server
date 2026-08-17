package com._penLearning.Noddi.domain.auth.service;

import com._penLearning.Noddi.domain.auth.dto.AuthRequestDto;
import com._penLearning.Noddi.domain.organization.entity.Organization;
import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.security.JwtProvider;
import com._penLearning.Noddi.global.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthSignupProfileServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private RedisUtil redisUtil;
    @Mock private AuthRequestDto.SignupRequestDto request;
    @Mock private Organization organization;

    @Test
    void savesDepartmentAndPositionDuringSignup() {
        AuthService service = new AuthService(
                userRepository,
                passwordEncoder,
                organizationRepository,
                jwtProvider,
                redisUtil
        );
        when(request.getOrganizationId()).thenReturn(1L);
        when(request.getEmail()).thenReturn("member@noddi.com");
        when(request.getPassword()).thenReturn("Password1!");
        when(request.getName()).thenReturn("김노디");
        when(request.getDepartment()).thenReturn("  백엔드팀  ");
        when(request.getPosition()).thenReturn("  백엔드 개발자  ");
        when(userRepository.existsByEmail("member@noddi.com")).thenReturn(false);
        when(redisUtil.getData("EMAIL_VERIFIED:1:member@noddi.com")).thenReturn("true");
        when(organizationRepository.findById(1L)).thenReturn(Optional.of(organization));
        when(passwordEncoder.encode("Password1!")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "userId", 10L);
            return user;
        });

        var response = service.signup(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getDepartment()).isEqualTo("백엔드팀");
        assertThat(userCaptor.getValue().getPosition()).isEqualTo("백엔드 개발자");
        assertThat(response.getUserId()).isEqualTo(10L);
        verify(redisUtil).deleteData("EMAIL_VERIFIED:1:member@noddi.com");
    }
}
