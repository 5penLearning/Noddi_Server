package com._penLearning.Noddi.domain.home.service;

import com._penLearning.Noddi.domain.home.code.HomeErrorCode;
import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerResponseDto;
import com._penLearning.Noddi.domain.home.repository.HomeAiAnswerDetailRepository;
import com._penLearning.Noddi.domain.home.repository.HomeAiAnswerRepository;
import com._penLearning.Noddi.domain.notification.entity.Notification;
import com._penLearning.Noddi.domain.notification.entity.NotificationReferenceType;
import com._penLearning.Noddi.domain.notification.entity.NotificationType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeAiAnswerQueryService {

    private final HomeAiAnswerRepository homeAiAnswerRepository;
    private final HomeAiAnswerDetailRepository homeAiAnswerDetailRepository;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;

    /** 홈 프로젝트 탭에 표시할 미확인 AI 답변 개수를 조회한다. */
    public List<HomeAiAnswerResponseDto.ProjectStatus>
    getUnreadAnswerCountsByProject(Long userId) {
        Map<Long, Long> unreadCountByProjectId =
                homeAiAnswerRepository.findUnreadAiAnswerCountsByProject(
                        userId,
                        NotificationType.QA_AI_REVIEW_REQUIRED,
                        NotificationReferenceType.QA_QUESTION
                ).stream()
                .collect(Collectors.toMap(
                        projection -> projection.getProjectId(),
                        projection -> projection.getUnreadCount()
                ));

        return homeAiAnswerRepository.findProjectsByMemberUserId(userId)
                .stream()
                .map(project -> HomeAiAnswerResponseDto.ProjectStatus.of(
                        project,
                        unreadCountByProjectId.getOrDefault(
                                project.getProjectId(),
                                0L
                        )
                ))
                .toList();
    }

    /** 선택한 프로젝트의 미확인 AI 답변을 홈 카드 형태로 조회한다. */
    public HomeAiAnswerResponseDto.CardPage getUnreadAnswerCards(
            Long userId,
            Long projectId,
            int page,
            int size
    ) {
        validatePageRequest(page, size);
        validateProjectMembership(userId, projectId);

        Page<Notification> notificationPage =
                homeAiAnswerRepository.findUnreadAiAnswerNotifications(
                        userId,
                        projectId,
                        NotificationType.QA_AI_REVIEW_REQUIRED,
                        NotificationReferenceType.QA_QUESTION,
                        PageRequest.of(page, size)
                );

        List<Long> questionIds = notificationPage.getContent().stream()
                .map(Notification::getReferenceId)
                .toList();

        List<QaAnswer> answers = findAnswers(questionIds);

        Map<Long, QaAnswer> answerByQuestionId = answers
                .stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getQuestionId(),
                        Function.identity()
                ));

        Map<Long, List<QaAnswerSource>> sourcesByAnswerId =
                findSources(answers).stream()
                        .collect(Collectors.groupingBy(
                                source -> source.getAnswer().getAnswerId()
                        ));

        List<HomeAiAnswerResponseDto.Card> cards =
                notificationPage.getContent().stream()
                        .map(notification -> {
                            QaAnswer answer = getAnswerOrThrow(
                                    answerByQuestionId,
                                    notification.getReferenceId()
                            );

                            return HomeAiAnswerResponseDto.Card.of(
                                    notification,
                                    answer,
                                    sourcesByAnswerId.getOrDefault(
                                            answer.getAnswerId(),
                                            List.of()
                                    )
                            );
                        })
                        .toList();

        return HomeAiAnswerResponseDto.CardPage.builder()
                .items(cards)
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .hasNext(notificationPage.hasNext())
                .hasPrevious(notificationPage.hasPrevious())
                .build();
    }

    private List<QaAnswer> findAnswers(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }

        return homeAiAnswerDetailRepository
                .findAllCardDetailsByQuestionIds(questionIds);
    }

    private List<QaAnswerSource> findSources(List<QaAnswer> answers) {
        if (answers.isEmpty()) {
            return List.of();
        }

        return qaAnswerSourceRepository
                .findAllByAnswersOrderByCitationIndex(answers);
    }

    private QaAnswer getAnswerOrThrow(
            Map<Long, QaAnswer> answerByQuestionId,
            Long questionId
    ) {
        QaAnswer answer = answerByQuestionId.get(questionId);
        if (answer == null) {
            throw new GeneralException(
                    HomeErrorCode.AI_ANSWER_DATA_NOT_FOUND
            );
        }
        return answer;
    }

    private void validateProjectMembership(Long userId, Long projectId) {
        if (!homeAiAnswerRepository.existsProjectMembership(
                projectId,
                userId
        )) {
            throw new GeneralException(HomeErrorCode.NOT_PROJECT_MEMBER);
        }
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 50) {
            throw new GeneralException(HomeErrorCode.INVALID_PAGE_REQUEST);
        }
    }
}
