package com._penLearning.Noddi.domain.teamPage.entity;


import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "TeamPage",
        indexes = @Index(
                name = "IDX_TEAM_PAGE_TEAM_UPDATED_PAGE",
                columnList = "teamId, updatedAt, pageId"
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamPage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamId", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorId", nullable = false)
    private User author;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    public void update(String title, String content){
        this.title = title;
        this.content = content;
    }

    @Builder
    public TeamPage(Team team, User author, String title, String content){
        this.team = team;
        this.author = author;
        this.title = title;
        this.content = content;
    }
}
