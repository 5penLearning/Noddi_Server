package com._penLearning.Noddi.domain.notification.message;

import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationMessageFactoryTest {

    private final NotificationMessageFactory factory =
            new NotificationMessageFactory();

    @Test
    void createsQaMessagesByNotificationType() {
        // Q&A 알림 타입마다 프로젝트·팀 접두사와 노출 문구가 정확히 생성되는지 확인한다.
        assertThat(factory.createQaMessage(
                NotificationType.QA_ANSWERED,
                "노디프로젝트",
                "마케팅팀"
        )).isEqualTo("[노디프로젝트/마케팅팀] 질문에 답변이 등록됐어요.");

        assertThat(factory.createQaMessage(
                NotificationType.QA_AI_REVIEW_REQUIRED,
                "노디프로젝트",
                "마케팅팀"
        )).isEqualTo("[노디프로젝트/마케팅팀] AI가 대신 답변했어요. 답변을 확인해 주세요.");

        assertThat(factory.createQaMessage(
                NotificationType.QA_ANSWER_WAITING,
                "노디프로젝트",
                "마케팅팀"
        )).isEqualTo("[노디프로젝트/마케팅팀] 담당 팀원이 질문을 확인하고 있어요.");

        assertThat(factory.createQaMessage(
                NotificationType.QA_AI_FAILED,
                "노디프로젝트",
                "마케팅팀"
        )).isEqualTo("[노디프로젝트/마케팅팀] AI 답변 생성에 실패했어요. 직접 답변해 주세요.");

        assertThat(factory.createQaMessage(
                NotificationType.QA_ANSWER_REVISED,
                "노디프로젝트",
                "마케팅팀"
        )).isEqualTo("[노디프로젝트/마케팅팀] 답변 내용이 수정됐어요.");
    }

    @Test
    void createsGroupedQaReviewMessageWithUnreadCount() {
        // 저장된 질문별 알림을 조회 단계에서 묶었을 때 안 읽은 질문 개수를 문구에 포함한다.
        String message = factory.createQaReviewGroupMessage(
                "노디프로젝트",
                "마케팅팀",
                5
        );

        assertThat(message)
                .isEqualTo("[노디프로젝트/마케팅팀] AI가 내가 없는 사이에 5개의 답변을 했어요.");
    }

    @Test
    void createsInviteMessagesUsingCurrentTemplates() {
        // 현재 합의한 초대 알림 문구를 그대로 고정해 이후 의도치 않은 변경을 방지한다.
        assertThat(factory.createTeamInviteMessage(
                "노디프로젝트",
                "마케팅팀",
                "홍길동"
        )).isEqualTo("[노디프로젝트/마케팅팀] 홍길동님이 팀에 초대했어요.");

        assertThat(factory.createProjectInviteMessage(
                "노디프로젝트",
                "홍길동"
        )).isEqualTo("[노디프로젝트] 홍길동님이 프로젝트에 초대했어요.");
    }

    @Test
    void createsActionItemAssignedMessage() {
        // 수동 배정과 AI 자동 배정이 같은 문구를 사용하도록 생성자 이름 없이 구성한다.
        String message = factory.createActionItemAssignedMessage(
                "노디프로젝트",
                "마케팅팀"
        );

        assertThat(message)
                .isEqualTo("[노디프로젝트/마케팅팀] 새로운 할 일이 할당됐어요.");
    }

    @Test
    void rejectsNonQaTypeForQaMessage() {
        // 초대 타입을 Q&A 메시지 생성기로 전달하는 개발 오류는 즉시 드러내야 한다.
        assertThatThrownBy(() -> factory.createQaMessage(
                NotificationType.TEAM_INVITE,
                "노디프로젝트",
                "마케팅팀"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 Q&A 알림 타입");
    }
}
