package com._penLearning.Noddi.domain.actionItem.repository;

import com._penLearning.Noddi.domain.actionItem.entity.ActionItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {
    List<ActionItem> findByMeetingSummary_SummaryId(Long summaryId);
    List<ActionItem> findByAssignee_UserId(Long userId);
}
