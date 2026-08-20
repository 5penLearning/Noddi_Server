package com._penLearning.Noddi.domain.qa.entity;

import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 지식 소스별 Pinecone 색인 상태를 MySQL에 기록한다.
 * 회의 전사와 팀 작성 텍스트를 같은 구조로 관리하며, 원문 해시로 중복 임베딩을 방지한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "QaKnowledgeIndex",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_qa_knowledge_index_source",
                columnNames = {"sourceId", "sourceType"}
        )
)
public class QaKnowledgeIndex extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long indexId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teamId", nullable = false)
    private Team team;

    @Column(name = "sourceId", nullable = false)
    private Long sourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sourceType", nullable = false)
    private SourceType sourceType;

    @Column(nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false)
    private int chunkCount;

    public QaKnowledgeIndex(
            Team team,
            Long sourceId,
            SourceType sourceType,
            String contentHash,
            int chunkCount
    ) {
        this.team = team;
        this.sourceId = sourceId;
        this.sourceType = sourceType;
        this.contentHash = contentHash;
        this.chunkCount = chunkCount;
    }

    public void update(String contentHash, int chunkCount) {
        this.contentHash = contentHash;
        this.chunkCount = chunkCount;
    }
}
