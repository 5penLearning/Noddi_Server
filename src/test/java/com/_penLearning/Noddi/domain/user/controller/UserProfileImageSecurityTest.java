package com._penLearning.Noddi.domain.user.controller;

import com._penLearning.Noddi.domain.user.service.UserProfileImageService;
import com._penLearning.Noddi.domain.user.service.UserService;
import com._penLearning.Noddi.domain.user.storage.ProfileImageResource;
import com._penLearning.Noddi.global.security.JwtAccessDeniedHandler;
import com._penLearning.Noddi.global.security.JwtAuthenticationEntryPoint;
import com._penLearning.Noddi.global.security.JwtProvider;
import com._penLearning.Noddi.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationEntryPoint.class,
        JwtAccessDeniedHandler.class
})
class UserProfileImageSecurityTest {

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47,
            0x0D, 0x0A, 0x1A, 0x0A
    };

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserProfileImageService userProfileImageService;

    /*
     * SecurityConfig가 생성하는 JWT 필터의 의존성이다.
     * 이 테스트는 토큰 파싱이 아니라 URL 접근 정책을 검증하므로 Mock으로 대체한다.
     */
    @MockitoBean
    private JwtProvider jwtProvider;

    /*
     * 메인 애플리케이션의 @EnableJpaAuditing이 웹 슬라이스에도 적용되지만
     * @WebMvcTest는 Entity를 로딩하지 않는다. 빈 JPA 메타모델 생성 실패를 막기 위한 테스트 Bean이다.
     */
    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void rejectsProfileImageRequestFromUnauthenticatedUser() throws Exception {
        /*
         * 과거 permitAll 설정이 다시 추가되는 회귀를 방지한다.
         * 인증 정보가 없으면 컨트롤러와 S3 조회 서비스까지 도달하지 않고 401이어야 한다.
         */
        mockMvc.perform(
                        get("/api/v1/users/{userId}/profile-image", 10L)
                )
                .andExpect(status().isUnauthorized());

        verify(userProfileImageService, never())
                .getProfileImage(10L);
    }

    @Test
    void returnsPrivateCachedImageToAuthenticatedUser() throws Exception {
        /*
         * 로그인 사용자는 이미지를 조회할 수 있어야 한다.
         * 공유 프록시가 인증 이미지를 보관하지 않도록 Cache-Control에는 private이 포함되어야 한다.
         */
        when(userProfileImageService.getProfileImage(10L))
                .thenReturn(
                        new ProfileImageResource(
                                new ByteArrayResource(PNG_BYTES),
                                MediaType.IMAGE_PNG
                        )
                );

        mockMvc.perform(
                        get("/api/v1/users/{userId}/profile-image", 10L)
                                .with(user("authenticated-member"))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES))
                .andExpect(header().string(
                        HttpHeaders.CACHE_CONTROL,
                        "max-age=31536000, private, immutable"
                ));

        verify(userProfileImageService).getProfileImage(10L);
    }
}
