package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.project.repository.ProjectMemberRepository;
import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerSource;
import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerSourceRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 특정 답변의 전체 수정 이력을 조회한다.
 *
 * 같은 프로젝트 구성원은 수정 이력의 내용은 모두 볼 수 있지만,
 * AI 답변의 출처는 질문 대상 팀 구성원에게만 제공한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QaAnswerRevisionQueryService {


    private final QaAnswerRepository qaAnswerRepository;
    private final QaAnswerRevisionRepository qaAnswerRevisionRepository;
    private final QaAnswerSourceRepository qaAnswerSourceRepository;

    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TeamMemberRepository teamMemberRepository;

    public QaResponseDto.AnswerRevisionHistory getAnswerRevisions(Long requesterId, Long answerId) {

        QaAnswer answer = qaAnswerRepository.findByIdWithQuestionTeamAndProject(answerId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.ANSWER_NOT_FOUND));

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        QaQuestion question = answer.getQuestion();
        Team targetTeam = question.getTargetTeam();

        boolean isProjectMember = projectMemberRepository.existsByProjectAndUser(targetTeam.getProject(), requester);

        if (!isProjectMember) {
            throw new GeneralException(QaErrorCode.NOT_PROJECT_MEMBER);
        }
        boolean canViewSources = teamMemberRepository.existsByTeamAndUser(targetTeam, requester);

        /*
         * Repository 쿼리에서 versionNumber 오름차순으로 조회하므로
         * 결과는 v1, v2, v3 순서다.
         *
         * HUMAN 버전의 revisedBy도 fetch join으로 함께 조회된다.
         */
        List<QaAnswerRevision> revisions = qaAnswerRevisionRepository.findAllByAnswerWithReviserOrderByVersionNumberAsc(answer);

        if(revisions.isEmpty()){
            throw new GeneralException(QaErrorCode.REVISION_HISTORY_NOT_FOUND);
        }

        List<QaAnswerSource> initialSources = canViewSources
                ? qaAnswerSourceRepository.findByAnswer_AnswerIdOrderByCitationIndexAsc(answer.getAnswerId())
                : List.of();

        return QaResponseDto.AnswerRevisionHistory.of(
                answer,
                revisions,
                initialSources,
                canViewSources);
    }
}
