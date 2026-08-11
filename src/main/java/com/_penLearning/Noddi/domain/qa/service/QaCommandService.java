package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.AnswerType;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QaCommandService {

    private final QaQuestionRepository qaQuestionRepository;
    private final QaAnswerRepository qaAnswerRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamMemberRepository teamMemberRepository;

    // 질문 등록
    public QaResponseDto.CreateQuestion createQuestion(Long questionerId, QaRequestDto.CreateQuestion request) {
        User questioner = userRepository.findById(questionerId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        Team targetTeam = teamRepository.findById(request.getTargetTeamId())
                .orElseThrow(() -> new GeneralException(TeamErrorCode.TEAM_NOT_FOUND));

        // 방어 로직: 질문자는 대상 팀이 속한 '프로젝트'의 멤버여야 함
        if (!projectMemberRepository.existsByProjectAndUser(targetTeam.getProject(), questioner)) {
            throw new GeneralException(QaErrorCode.NOT_PROJECT_MEMBER);
        }

        QaQuestion question = QaQuestion.builder()
                .questioner(questioner)
                .targetTeam(targetTeam)
                .content(request.getContent())
                .build();

        return QaResponseDto.CreateQuestion.from(qaQuestionRepository.save(question).getQuestionId());
    }

    // 직접 답변 등록
    public QaResponseDto.CreateAnswer createAnswer(Long questionId, Long answererId, QaRequestDto.CreateAnswer request) {
        QaQuestion question = qaQuestionRepository.findByIdWithLock(questionId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.QUESTION_NOT_FOUND));

        // 방어 로직: 이미 답변이 달렸거나 PENDING 상태가 아닌 경우
        if (question.getStatus() != QaStatus.PENDING) {
            throw new GeneralException(QaErrorCode.ALREADY_ANSWERED);
        }

        User answerer = userRepository.findById(answererId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        // 방어 로직: 답변자는 질문 대상 '팀'의 멤버여야 함
        if (!teamMemberRepository.existsByTeamAndUser(question.getTargetTeam(), answerer)) {
            throw new GeneralException(QaErrorCode.NOT_TARGET_TEAM_MEMBER);
        }

        // 방어 로직: 혹시 모를 중복 답변 생성 방지
        if (qaAnswerRepository.existsByQuestion(question)) {
            throw new GeneralException(QaErrorCode.ALREADY_ANSWERED);
        }

        QaAnswer answer = QaAnswer.builder()
                .question(question)
                .content(request.getContent())
                .answerType(AnswerType.HUMAN) // HUMAN 명시
                .answeredBy(answerer)
                .build();

        QaAnswer savedAnswer = qaAnswerRepository.save(answer);

        // 질문 상태를 ANSWERED로 업데이트 (더티 체킹)
        question.markAsAnswered();
        return QaResponseDto.CreateAnswer.from(savedAnswer.getAnswerId());
    }
}
