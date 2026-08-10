package com._penLearning.Noddi.domain.team.service;

import com._penLearning.Noddi.domain.team.code.TeamErrorCode;
import com._penLearning.Noddi.domain.team.entity.TeamInvite;
import com._penLearning.Noddi.domain.team.repository.TeamInviteRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TeamInviteExpirationService {

    private final TeamInviteRepository teamInviteRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(Long inviteId) {
        TeamInvite invite = teamInviteRepository.findById(inviteId)
                .orElseThrow(() -> new GeneralException(TeamErrorCode.INVITE_NOT_FOUND));

        invite.expire();
    }
}
