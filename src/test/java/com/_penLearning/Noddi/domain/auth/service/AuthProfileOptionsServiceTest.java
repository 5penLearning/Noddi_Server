package com._penLearning.Noddi.domain.auth.service;

import com._penLearning.Noddi.domain.organization.repository.OrganizationRepository;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.security.JwtProvider;
import com._penLearning.Noddi.global.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthProfileOptionsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private JwtProvider jwtProvider;
    @Mock private RedisUtil redisUtil;

    @Test
    void returnsOrganizationProfileOptionsWithTwentyItemLimit() {
        AuthService service = service();
        when(organizationRepository.existsById(1L)).thenReturn(true);
        when(userRepository.findPopularDepartments(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of("백엔드팀", "기획팀"));
        when(userRepository.findPopularPositions(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.any(Pageable.class)
        )).thenReturn(List.of("백엔드 개발자", "서비스 기획자"));

        var response = service.getSignupProfileOptions(1L);

        assertThat(response.getDepartments()).containsExactly("백엔드팀", "기획팀");
        assertThat(response.getPositions()).containsExactly("백엔드 개발자", "서비스 기획자");

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findPopularDepartments(
                org.mockito.ArgumentMatchers.eq(1L),
                pageableCaptor.capture()
        );
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void rejectsUnknownOrganizationBeforeQueryingUserProfiles() {
        AuthService service = service();
        when(organizationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.getSignupProfileOptions(999L))
                .isInstanceOf(GeneralException.class);

        verifyNoInteractions(userRepository);
    }

    private AuthService service() {
        return new AuthService(
                userRepository,
                passwordEncoder,
                organizationRepository,
                jwtProvider,
                redisUtil
        );
    }
}
