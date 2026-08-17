package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.entity.QaAnswerRevision;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRevisionRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// 대상 팀 구성원이 AI 답변을 수정한다.
// QaAnswer에는 최신 답변을 반영하고,
// QaAnswerRevision에는 AI 원문부터 모든 수정본을 누적 보관한다.
@Service
@RequiredArgsConstructor
public class QaAnswerUpdateService {

    private final QaAnswerRepository qaAnswerRepository;
    private final QaAnswerRevisionRepository qaAnswerRevisionRepository;
    private final UserRepository userRepository;
    private final TeamMemberRepository teamMemberRepository;

    @Transactional
    public QaResponseDto.ReviseAnswer revise(
            Long answerId,
            Long reviserId,
            QaRequestDto.ReviseAnswer request
    ) {
        QaAnswer answer = qaAnswerRepository.findByIdWithQuestionAndTeamForUpdate(answerId)
                .orElseThrow(() -> new GeneralException(QaErrorCode.ANSWER_NOT_FOUND));

        User reviser = userRepository.findById(reviserId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        if (!teamMemberRepository.existsByTeamAndUser(answer.getQuestion().getTargetTeam(), reviser)) {
            throw new GeneralException(QaErrorCode.NOT_TARGET_TEAM_MEMBER);
        }

        QaAnswerRevision lastRevision = qaAnswerRevisionRepository.findTopByAnswerOrderByVersionNumberDesc(answer)
                .orElseThrow(() -> new GeneralException(QaErrorCode.REVISION_HISTORY_NOT_FOUND));

        int nextVersionNumber = lastRevision.getVersionNumber() + 1;

        QaAnswerRevision newRevision = QaAnswerRevision.createHumanRevision(
                answer,
                nextVersionNumber,
                request.getContent(),
                reviser
        );

        qaAnswerRevisionRepository.save(newRevision);

        answer.revise(request.getContent(), reviser);
        return QaResponseDto.ReviseAnswer.from(answer);
    }
}
