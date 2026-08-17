package com._penLearning.Noddi.domain.notification.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.notification.dto.NotificationFilter;
import com._penLearning.Noddi.domain.notification.dto.NotificationNavigationType;
import com._penLearning.Noddi.domain.notification.dto.NotificationResponseDto;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.notification.service.NotificationCommandService;
import com._penLearning.Noddi.domain.notification.service.NotificationQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    /*
     * 이 테스트는 HTTP 요청이 NotificationController를 거쳐
     * Query/Command 서비스 호출과 공통 ApiResponse로 이어지는지 확인한다.
     *
     * 알림 그룹화, 소유권 검증, 읽음·숨김 상태 변경 같은 비즈니스 규칙은
     * 각각 NotificationQueryServiceTest와 NotificationCommandServiceTest에서 검증한다.
     * 여기서는 URL, query parameter, JSON body, 인증 사용자 ID가
     * 서비스 인자로 정확하게 전달되는지를 중점적으로 확인한다.
     */

    @Mock
    private NotificationQueryService notificationQueryService;

    @Mock
    private NotificationCommandService notificationCommandService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        NotificationController controller = new NotificationController(
                notificationQueryService,
                notificationCommandService
        );

        // 실제 JWT 필터 대신 로그인 완료 후 SecurityContext에 들어갈 principal을 준비한다.
        // 컨트롤러의 @AuthenticationPrincipal AuthMember 인자로 이 객체가 전달된다.
        AuthMember authMember = AuthMember.builder()
                .userId(1L)
                .email("member@noddi.com")
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

        // 전체 Spring 애플리케이션을 실행하지 않고 컨트롤러만 연결해
        // HTTP 매핑과 JSON 직렬화 테스트를 빠르게 수행한다.
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();
    }

    @AfterEach
    void tearDown() {
        // SecurityContextHolder는 ThreadLocal 기반이므로 테스트 간 인증 정보 누수를 막는다.
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsNotificationsWithDefaultFilterAndPagination() throws Exception {
        // Given: 사용자가 처음 알림함에 들어와 필터와 페이지 조건을 생략한 상황이다.
        NotificationResponseDto.NotificationList response =
                createNotificationList();

        when(notificationQueryService.getNotifications(
                1L,
                NotificationFilter.ALL,
                0,
                20
        )).thenReturn(response);

        // When & Then: 컨트롤러 기본값 ALL, 0, 20이 적용되고
        // 프론트가 표시 및 이동에 사용할 알림 데이터가 공통 응답으로 반환된다.
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message")
                        .value("알림 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(20))
                .andExpect(jsonPath("$.result.unreadCount").value(1))
                .andExpect(jsonPath("$.result.items[0].notificationId")
                        .value(501L))
                .andExpect(jsonPath("$.result.items[0].type")
                        .value("QA_ANSWERED"))
                .andExpect(jsonPath("$.result.items[0].navigation.type")
                        .value("QA_QUESTION"))
                .andExpect(jsonPath("$.result.items[0].navigation.referenceId")
                        .value(101L));

        // 인증 사용자 ID와 컨트롤러 기본값이 서비스에 정확히 전달됐는지 확인한다.
        verify(notificationQueryService).getNotifications(
                1L,
                NotificationFilter.ALL,
                0,
                20
        );
    }

    @Test
    void passesUnreadFilterAndPaginationToQueryService() throws Exception {
        // Given: 사용자가 안 읽은 알림 탭의 두 번째 페이지를 조회한다.
        when(notificationQueryService.getNotifications(
                1L,
                NotificationFilter.UNREAD,
                2,
                10
        )).thenReturn(
                NotificationResponseDto.NotificationList.builder()
                        .items(List.of())
                        .page(2)
                        .size(10)
                        .totalElements(20)
                        .totalPages(2)
                        .hasNext(false)
                        .unreadCount(20)
                        .build()
        );

        // When: filter, page, size를 query parameter로 명시한다.
        mockMvc.perform(get("/api/v1/notifications")
                        .param("filter", "UNREAD")
                        .param("page", "2")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.page").value(2))
                .andExpect(jsonPath("$.result.size").value(10));

        // Then: 문자열 UNREAD가 enum으로 변환되고 페이지 값도 그대로 전달된다.
        verify(notificationQueryService).getNotifications(
                1L,
                NotificationFilter.UNREAD,
                2,
                10
        );
    }

    @Test
    void marksOwnedNotificationAsRead() throws Exception {
        // When: 개별 알림의 자세히보기를 눌러 읽음 API를 호출한다.
        mockMvc.perform(patch(
                        "/api/v1/notifications/{notificationId}/read",
                        501L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.message")
                        .value("알림을 읽음 처리했습니다."));

        // Then: 인증 사용자 ID와 URL의 notificationId가 서비스에 전달된다.
        verify(notificationCommandService).markAsRead(1L, 501L);
    }

    @Test
    void marksReviewNotificationGroupAsRead() throws Exception {
        // Given: 프론트가 안 읽은 AI 답변 검토 묶음의 자세히보기를 누른다.
        String request = """
                {
                  "projectId": 100,
                  "teamId": 10,
                  "type": "QA_AI_REVIEW_REQUIRED"
                }
                """;

        // When: 묶음을 식별하는 프로젝트·팀·알림 타입을 JSON body로 전달한다.
        mockMvc.perform(patch("/api/v1/notifications/groups/read")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("묶음 알림을 읽음 처리했습니다."));

        // Then: 현재 사용자에게 속한 해당 묶음을 읽음 처리하도록 서비스가 호출된다.
        verify(notificationCommandService).markGroupAsRead(
                1L,
                100L,
                10L,
                NotificationType.QA_AI_REVIEW_REQUIRED
        );
    }

    @Test
    void hidesOwnedNotification() throws Exception {
        // When: 개별 알림의 X 버튼을 눌러 숨김 API를 호출한다.
        mockMvc.perform(patch(
                        "/api/v1/notifications/{notificationId}/hide",
                        501L
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("알림을 숨김 처리했습니다."));

        // Then: 다른 사용자의 알림을 숨길 수 없도록 인증 사용자 ID도 함께 전달된다.
        verify(notificationCommandService).hide(1L, 501L);
    }

    @Test
    void hidesOnlySelectedReadStatusGroup() throws Exception {
        // Given: ALL 목록에서 읽은 AI 검토 묶음의 X 버튼을 누른 상황이다.
        // read=true가 없으면 같은 프로젝트·팀의 안 읽은 묶음까지 숨길 수 있으므로 반드시 전달한다.
        String request = """
                {
                  "projectId": 100,
                  "teamId": 10,
                  "type": "QA_AI_REVIEW_REQUIRED",
                  "read": true
                }
                """;

        // When: 묶음 숨김 API를 호출한다.
        mockMvc.perform(patch("/api/v1/notifications/groups/hide")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("묶음 알림을 숨김 처리했습니다."));

        // Then: 화면에서 선택한 읽음 상태까지 서비스에 그대로 전달된다.
        verify(notificationCommandService).hideGroup(
                1L,
                100L,
                10L,
                NotificationType.QA_AI_REVIEW_REQUIRED,
                true
        );
    }

    private NotificationResponseDto.NotificationList createNotificationList() {
        // Controller 응답 JSON 구조를 확인하기 위한 대표 개별 알림 데이터다.
        NotificationResponseDto.Navigation navigation =
                NotificationResponseDto.Navigation.builder()
                        .type(NotificationNavigationType.QA_QUESTION)
                        .projectId(100L)
                        .teamId(10L)
                        .referenceId(101L)
                        .build();

        NotificationResponseDto.NotificationItem item =
                NotificationResponseDto.NotificationItem.builder()
                        .notificationId(501L)
                        .groupKey(null)
                        .type(NotificationType.QA_ANSWERED)
                        .message("마케팅팀에서 질문에 답변했어요.")
                        .read(false)
                        .grouped(false)
                        .count(1)
                        .occurredAt(LocalDateTime.of(
                                2026,
                                8,
                                18,
                                10,
                                0
                        ))
                        .navigation(navigation)
                        .build();

        return NotificationResponseDto.NotificationList.builder()
                .items(List.of(item))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .hasNext(false)
                .unreadCount(1)
                .build();
    }
}
