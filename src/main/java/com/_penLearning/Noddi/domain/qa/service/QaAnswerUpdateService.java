package com._penLearning.Noddi.domain.qa.service;

import com._penLearning.Noddi.domain.qa.code.QaErrorCode;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.entity.QaAnswer;
import com._penLearning.Noddi.domain.qa.repository.QaAnswerRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


// 대상 팀 구성원이 AI 답변을 수정한다.
// 수정 전 원문은 별도로 보관하지 않고 현재 답변과 마지막 수정자만 갱신한다.
@Service
@RequiredArgsConstructor
public class QaAnswerUpdateService {

    private final QaAnswerRepository qaAnswerRepository;
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

        answer.revise(request.getContent(), reviser);
        return QaResponseDto.ReviseAnswer.from(answer);
    }
}
