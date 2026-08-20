package com._penLearning.Noddi.domain.teamPage.event;

/** 공유페이지 삭제 커밋 이후 Pinecone에 남은 청크 삭제를 요청한다. */
public record TeamPageDeletedEvent(Long pageId) {
}
