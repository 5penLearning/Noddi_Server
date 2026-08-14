package com._penLearning.Noddi.domain.actionItem.service;

import com._penLearning.Noddi.domain.actionItem.dto.ActionItemResponseDto;
import com._penLearning.Noddi.domain.actionItem.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.user.code.UserErrorCode;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActionItemQueryService {

    private final ActionItemRepository actionItemRepository;
    private final UserRepository userRepository;

    public List<ActionItemResponseDto.Info> getMyActionItems(Long currentUserId) {
        if (!userRepository.existsById(currentUserId)) {
            throw new GeneralException(UserErrorCode.USER_NOT_FOUND);
        }

        return actionItemRepository.findAllByAssigneeIdWithDetails(currentUserId)
                .stream()
                .map(ActionItemResponseDto.Info::from)
                .toList();
    }

}
