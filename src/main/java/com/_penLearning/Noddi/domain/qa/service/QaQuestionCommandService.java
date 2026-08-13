package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.event.QaQuestionCreatedEvent;
import com._penLearning.Noddi.domain.qa.repository.QaQuestionRepository;
import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Q&A 질문 등록을 담당한다.
 * 질문 저장 트랜잭션에서는 외부 AI를 호출하지 않고 생성 이벤트만 발행한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QaQuestionCommandService {

    private final QaQuestionRepository qaQuestionRepository;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

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

        QaQuestion savedQuestion = qaQuestionRepository.save(question);

        // AFTER_COMMIT 리스너가 이 이벤트를 받아 AI 답변 생성을 시작할 예정이다.
        // 질문 저장이 롤백되면 이벤트 후속 처리도 실행되지 않아야 한다.
        eventPublisher.publishEvent(new QaQuestionCreatedEvent(savedQuestion.getQuestionId()));
        return QaResponseDto.CreateQuestion.from(savedQuestion);
    }
}
