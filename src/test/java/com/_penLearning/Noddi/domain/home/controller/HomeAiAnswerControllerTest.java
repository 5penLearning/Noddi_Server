package com._penLearning.Noddi.domain.home.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerResponseDto;
import com._penLearning.Noddi.domain.home.service.HomeAiAnswerQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HomeAiAnswerControllerTest {

    @Mock
    private HomeAiAnswerQueryService homeAiAnswerQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HomeAiAnswerController controller = new HomeAiAnswerController(
                homeAiAnswerQueryService
        );

        AuthMember authMember = AuthMember.builder()
                .userId(1L)
                .email("reviewer@noddi.test")
                .build();

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        authMember,
                        null,
                        List.of()
                )
        );
        SecurityContextHolder.setContext(securityContext);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsProjectStatusesForTheAuthenticatedUser() throws Exception {
        HomeAiAnswerResponseDto.ProjectStatus status =
                HomeAiAnswerResponseDto.ProjectStatus.builder()
                        .projectId(10L)
                        .projectName("노디 프로젝트")
                        .unreadAnswerCount(3)
                        .build();
        when(homeAiAnswerQueryService.getUnreadAnswerCountsByProject(1L))
                .thenReturn(List.of(status));

        mockMvc.perform(get("/api/v1/home/ai-answers/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].projectId").value(10L))
                .andExpect(jsonPath("$.result[0].projectName")
                        .value("노디 프로젝트"))
                .andExpect(jsonPath("$.result[0].unreadAnswerCount")
                        .value(3));

        verify(homeAiAnswerQueryService)
                .getUnreadAnswerCountsByProject(1L);
    }

    @Test
    void usesDefaultPaginationWhenLoadingProjectCards() throws Exception {
        HomeAiAnswerResponseDto.CardPage response =
                HomeAiAnswerResponseDto.CardPage.builder()
                        .items(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .hasNext(false)
                        .hasPrevious(false)
                        .build();
        when(homeAiAnswerQueryService.getUnreadAnswerCards(
                1L,
                10L,
                0,
                10
        )).thenReturn(response);

        mockMvc.perform(get(
                        "/api/v1/home/ai-answers/projects/{projectId}",
                        10L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalElements").value(0));

        verify(homeAiAnswerQueryService).getUnreadAnswerCards(
                1L,
                10L,
                0,
                10
        );
    }
}
