package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        Team targetTeam = teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));

        validateProjectMembership(requesterId, targetTeam);

        return qaQuestionRepository.findAllByTargetTeamWithUser(targetTeam, pageable)
                .map(QaResponseDto.QuestionInfo::from);
    }

    // 질문 상세 조회
    public QaResponseDto.QuestionDetail getQuestionDetail(Long requesterId, Long questionId) {
        QaQuestion question = qaQuestionRepository.findByIdWithTeam(questionId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));

        validateProjectMembership(requesterId, question.getTargetTeam());

        QaAnswer answer = qaAnswerRepository.findByQuestion(question).orElse(null);
        return QaResponseDto.QuestionDetail.of(question, answer);
    }

    // 공통 프로젝트 권한 검증 로직
    private void validateProjectMembership(Long requesterId, Team targetTeam) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (!projectMemberRepository.existsByProjectAndUser(targetTeam.getProject(), requester)) {
            throw new GeneralException(QaErrorCode.NOT_PROJECT_MEMBER);
        }
    }
}
