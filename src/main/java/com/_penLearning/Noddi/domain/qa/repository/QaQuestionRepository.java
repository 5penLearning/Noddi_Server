package com._penLearning.Noddi.domain.qa.repository;

import com._penLearning.Noddi.domain.qa.entity.QaQuestion;
import com._penLearning.Noddi.domain.qa.entity.QaStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaQuestionRepository extends JpaRepository<QaQuestion, Long> {
    List<QaQuestion> findByQuestioner_UserIdOrderByCreatedAtDesc(Long userId);
    List<QaQuestion> findByTargetTeam_TeamIdOrderByCreatedAtDesc(Long teamId);
    List<QaQuestion> findByStatus(QaStatus status);
}
