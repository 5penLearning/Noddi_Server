package com._penLearning.Noddi.domain.summary.entity;

import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ActionItem")
public class ActionItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long actionItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meetingSummaryId", nullable = false)
    private MeetingSummary meetingSummary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigneeId")
    private User assignee;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private Boolean isUncertain = false;

    private LocalDate dueDate;

    @Builder
    public ActionItem(MeetingSummary meetingSummary, User assignee, String content, Boolean isUncertain, LocalDate dueDate) {
        this.meetingSummary = meetingSummary;
        this.assignee = assignee;
        this.content = content;
        this.isUncertain = isUncertain != null ? isUncertain : false;
        this.dueDate = dueDate;
    }

    public void assignTo(User user) {
        this.assignee = user;
    }
}
