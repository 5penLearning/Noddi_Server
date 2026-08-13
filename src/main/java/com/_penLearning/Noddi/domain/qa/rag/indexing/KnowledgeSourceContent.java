package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;

// 회의 전사와 팀 작성 텍스트를 청킹·임베딩 계층에 전달하는 공통 형식
public record KnowledgeSourceContent(
        Long sourceId,
        Long teamId,
        SourceType sourceType,
        String sourceTitle,
        String content
) {
}
