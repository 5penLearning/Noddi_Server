package com._penLearning.Noddi.domain.qa.entity;

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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 답변 생성에 실제로 사용된 근거를 저장한다.
 * 특정 원본 Entity를 직접 참조하지 않아 회의 전사와 팀 작성 텍스트를 같은 형태로 표현한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "QaAnswerSource",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_qa_answer_source_citation",
                columnNames = {"answerId", "citationIndex"}
        )
)
public class QaAnswerSource extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long answerSourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerId", nullable = false)
    private QaAnswer answer;

    @Column(nullable = false)
    private int citationIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SourceType sourceType;

    @Column(nullable = false)
    private Long referenceId;

    @Column(nullable = false)
    private String sourceTitle;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String excerpt;

    @Builder
    public QaAnswerSource(
            QaAnswer answer,
            int citationIndex,
            SourceType sourceType,
            Long referenceId,
            String sourceTitle,
            String excerpt
    ) {
        this.answer = answer;
        this.citationIndex = citationIndex;
        this.sourceType = sourceType;
        this.referenceId = referenceId;
        this.sourceTitle = sourceTitle;
        this.excerpt = excerpt;
    }
}
