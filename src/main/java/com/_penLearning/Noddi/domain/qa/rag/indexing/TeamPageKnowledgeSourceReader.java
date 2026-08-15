package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.teamPage.code.TeamPageErrorCode;
import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공유페이지를 Pinecone 인덱싱에 사용하는 공통 지식 원본 형식으로 변환한다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamPageKnowledgeSourceReader {

    private final TeamPageRepository teamPageRepository;

    public KnowledgeSourceContent read(Long pageId) {
        TeamPage page = teamPageRepository.findByPageIdWithTeam(pageId)
                .orElseThrow(() -> new GeneralException(TeamPageErrorCode.TEAM_PAGE_NOT_FOUND));

        return new KnowledgeSourceContent(
                page.getPageId(),
                page.getTeam().getTeamId(),
                SourceType.TEAM_TEXT,
                page.getTitle(),
                markdownContent(page)
        );
    }

    private String markdownContent(TeamPage page) {
        // 제목도 임베딩 대상에 포함해야 제목 검색과 제목만 변경된 경우의 재인덱싱이 가능하다.
        return "# %s\n\n%s".formatted(page.getTitle(), page.getContent());
    }
}
