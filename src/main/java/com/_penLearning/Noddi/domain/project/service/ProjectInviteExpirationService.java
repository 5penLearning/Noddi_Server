package com._penLearning.Noddi.domain.project.service;

import com._penLearning.Noddi.domain.project.code.ProjectErrorCode;
import com._penLearning.Noddi.domain.project.entity.ProjectInvite;
import com._penLearning.Noddi.domain.project.repository.ProjectInviteRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProjectInviteExpirationService {

    private final ProjectInviteRepository projectInviteRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void expire(Long inviteId) {
        ProjectInvite invite = projectInviteRepository.findById(inviteId)
                .orElseThrow(() -> new GeneralException(ProjectErrorCode.INVITE_NOT_FOUND));
        invite.expire();
    }
}
