package com._penLearning.Noddi.domain.notification.message;

import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import org.springframework.stereotype.Component;

@Component
public class NotificationMessageFactory {

    /**
     * Q&A 알림 문구를 생성한다.
     *
     * 이벤트 핸들러가 문자열을 직접 조립하지 않도록
     * 프로젝트명, 팀명, 알림 타입에 따른 문구를 이 클래스에서 관리한다.
     */
    public String createQaMessage(
            NotificationType type,
            String projectName,
            String teamName
    ) {
        String prefix = createProjectTeamPrefix(projectName, teamName);

        return switch (type) {
            case QA_ANSWERED ->
                    prefix + " 질문에 답변이 등록됐어요.";

            case QA_AI_REVIEW_REQUIRED ->
                    prefix + " AI가 대신 답변했어요. 답변을 확인해 주세요.";

            case QA_ANSWER_WAITING ->
                    prefix + " 담당 팀원이 질문을 확인하고 있어요.";

            case QA_AI_FAILED ->
                    prefix + " AI 답변 생성에 실패했어요. 직접 답변해 주세요.";

            case QA_ANSWER_REVISED ->
                    prefix + " 답변 내용이 수정됐어요.";

            default -> throw new IllegalArgumentException(
                    "지원하지 않는 Q&A 알림 타입입니다: " + type
            );
        };
    }

    /**
     * 팀 초대 알림 문구를 생성한다.
     *
     * 초대 기능을 연결할 때 이벤트 핸들러에서 사용한다.
     */
    public String createTeamInviteMessage(
            String projectName,
            String teamName,
            String inviterName
    ) {
        return createProjectTeamPrefix(projectName, teamName)
                + " "
                + inviterName
                + "님이 팀에 초대했어요.";
    }

    /**
     * 프로젝트 초대 알림 문구를 생성한다.
     *
     * 프로젝트 초대는 소속 팀이 없으므로 프로젝트명만 표시한다.
     */
    public String createProjectInviteMessage(
            String projectName,
            String inviterName
    ) {
        return "["
                + projectName
                + "] "
                + inviterName
                + "님이 프로젝트에 초대했어요.";
    }

    private String createProjectTeamPrefix(
            String projectName,
            String teamName
    ) {
        return "["
                + projectName
                + "/"
                + teamName
                + "]";
    }

    /**
     * 알림함에서 같은 프로젝트·팀의 AI 검토 알림을 묶어 보여줄 때 사용한다.
     *
     * unreadCount는 저장 시점이 아니라 알림함 조회 시 계산한 값이다.
     */
    public String createQaReviewGroupMessage(
            String projectName,
            String teamName,
            long unreadCount
    ) {
        return createProjectTeamPrefix(projectName, teamName)
                + " AI가 내가 없는 사이에 "
                + unreadCount
                + "개의 답변을 했어요.";
    }

    /**
     * Action Item 담당자 배정 알림 문구를 생성한다.
     *
     * 수동 배정과 AI 자동 배정에서 공통으로 사용하므로
     * 배정한 사용자의 이름은 문구에 포함하지 않는다.
     */
    public String createActionItemAssignedMessage(
            String projectName,
            String teamName
    ) {
        return createProjectTeamPrefix(
                projectName,
                teamName
        ) + " 새로운 할 일이 할당됐어요.";
    }
}