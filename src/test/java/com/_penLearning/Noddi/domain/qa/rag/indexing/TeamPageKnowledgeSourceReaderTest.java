package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.teamPage.entity.TeamPage;
import com._penLearning.Noddi.domain.teamPage.repository.TeamPageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamPageKnowledgeSourceReaderTest {

    @Mock
    private TeamPageRepository teamPageRepository;
    @Mock
    private TeamPage page;
    @Mock
    private Team team;

    @InjectMocks
    private TeamPageKnowledgeSourceReader sourceReader;

    @Test
    void convertsMarkdownPageToTeamTextKnowledgeSource() {
        when(teamPageRepository.findByPageIdWithTeam(30L)).thenReturn(Optional.of(page));
        when(page.getPageId()).thenReturn(30L);
        when(page.getTeam()).thenReturn(team);
        when(team.getTeamId()).thenReturn(10L);
        when(page.getTitle()).thenReturn("배포 규칙");
        when(page.getContent()).thenReturn("## 일정\n- 금요일 배포");

        KnowledgeSourceContent source = sourceReader.read(30L);

        assertThat(source.sourceId()).isEqualTo(30L);
        assertThat(source.teamId()).isEqualTo(10L);
        assertThat(source.sourceType()).isEqualTo(SourceType.TEAM_TEXT);
        assertThat(source.sourceTitle()).isEqualTo("배포 규칙");
        assertThat(source.content()).isEqualTo("# 배포 규칙\n\n## 일정\n- 금요일 배포");
    }
}
