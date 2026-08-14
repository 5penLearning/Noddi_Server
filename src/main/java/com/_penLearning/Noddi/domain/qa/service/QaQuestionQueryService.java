package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Q&A 피드와 질문 상세 조회를 담당한다.
 * 상세 조회에서는 수정 전 원문 없이 현재 최종 답변과 마지막 수정자만 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaQuestionQueryService {

    private final QaQuestionRepository qaQuestionRepository;
    private final QaAnswerRepository qaAnswerRepository;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectMemberRepository projectMemberRepository;

    // 내가 작성한 질문 목록 조회
    public Page<QaResponseDto.QuestionInfo> getMyQuestions(Long userId, Pageable pageable) {
        User questioner = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        return qaQuestionRepository.findAllByQuestionerWithTeam(questioner, pageable)
                .map(QaResponseDto.QuestionInfo::from);
    }

    // 특정 팀에 등록된 질문 조회
    public Page<QaResponseDto.QuestionInfo> getTeamQuestions(Long requesterId, Long teamId, Pageable pageable) {
        Team targetTeam = getTeamOrThrow(teamId);

        validateProjectMembership(requesterId, targetTeam);

        return qaQuestionRepository.findAllByTargetTeamWithUser(targetTeam, pageable)
                .map(QaResponseDto.QuestionInfo::from);
    }

    public QaResponseDto.Feed  getTeamFeed(Long requesterId, Long teamId, Long cursor, int size) {
        validateFeedRequest(cursor, size);

        Team targetTeam = getTeamOrThrow(teamId);

        validateProjectMembership(requesterId, targetTeam);

        List<QaQuestion> fetchedQuestions = qaQuestionRepository.findFeedByTargetTeam(
                targetTeam, cursor, PageRequest.of(0, size + 1));

        boolean hasNext = fetchedQuestions.size() > size;

        List<QaQuestion> questions = fetchedQuestions.stream()
                .limit(size)
                .toList();

        Long nextCursor = calculateNextCursor(questions, hasNext);

        List<QaAnswer> answers = findAnswers(questions);

        Map<Long, QaAnswer> answerByQuestionId = answers.stream()
                .collect(Collectors.toMap(
                        answer -> answer.getQuestion().getQuestionId(),
                        Function.identity()
                ));

        List<QaAnswerSource> sources = findSources(answers);

        Map<Long, List<QaAnswerSource>> sourceByAnswerId = sources.stream()
                .collect(Collectors.groupingBy(
                        source -> source.getAnswer().getAnswerId()
                ));

        List<QaResponseDto.FeedItem> items = questions.stream()
                .map(question -> {
                    QaAnswer answer = answerByQuestionId.get(question.getQuestionId());

                    List<QaAnswerSource> answerSources =
                            answer == null ? List.of() : sourceByAnswerId.get(answer.getAnswerId());

                    return QaResponseDto.FeedItem.of(question, answer, answerSources);
                })
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.reverse(items);

        return QaResponseDto.Feed.of(targetTeam, items, nextCursor, hasNext);

    }

    // 질문 상세 조회
    public QaResponseDto.QuestionDetail getQuestionDetail(Long requesterId, Long questionId) {
        QaQuestion question = qaQuestionRepository.findByIdWithTeam(questionId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));

        validateProjectMembership(requesterId, question.getTargetTeam());

        QaAnswer answer = qaAnswerRepository.findByQuestion(question).orElse(null);
        List<QaAnswerSource> sources = answer == null
                ? List.of()
                : qaAnswerSourceRepository.findByAnswer_AnswerIdOrderByCitationIndexAsc(answer.getAnswerId());
        return QaResponseDto.QuestionDetail.of(question, answer, sources);
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));
    }

    // 공통 프로젝트 권한 검증 로직
    private void validateProjectMembership(Long requesterId, Team targetTeam) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (!projectMemberRepository.existsByProjectAndUser(targetTeam.getProject(), requester)) {
            throw new GeneralException(QaErrorCode.NOT_PROJECT_MEMBER);
        }
    }

    private void validateFeedRequest(Long cursor, int size) {
        if (size < 1 || size > 50) {
            throw new GeneralException(QaErrorCode.INVALID_FEED_SIZE);
        }

        if (cursor != null && cursor < 1) {
            throw new GeneralException(QaErrorCode.INVALID_FEED_CURSOR);
        }
    }

    private Long calculateNextCursor(List<QaQuestion> questions, boolean hasNext)
    {
        if (!hasNext || questions.isEmpty()) {
            return null;
        }

        return questions.get(questions.size() - 1).getQuestionId();
    }

    private List<QaAnswer> findAnswers(List<QaQuestion> questions)
    {
        if (questions.isEmpty()) {
            return List.of();
        }

        return qaAnswerRepository.findAllByQuestionsWithReviser(questions);
    }

    private List<QaAnswerSource> findSources(List<QaAnswer> answers)
    {
        if (answers.isEmpty()) {
            return List.of();
        }

        return qaAnswerSourceRepository.findAllByAnswersOrderByCitationIndex(answers);
    }
}
