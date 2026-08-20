package com._penLearning.Noddi.domain.teamPage.event;

/** 공유페이지 생성 또는 수정 커밋 이후 Pinecone 동기화를 요청한다. */
public record TeamPageChangedEvent(Long pageId) {
}
