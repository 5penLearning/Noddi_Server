package com._penLearning.Noddi.domain.qa.rag.generation;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.qa.rag.retrieval.RetrievedKnowledge;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QaRagPromptFactoryTest {

    private final QaRagPromptFactory promptFactory = new QaRagPromptFactory();

    @Test
    void createsPromptFromQuestionAndRetrievedTeamKnowledge() {
        List<RetrievedKnowledge> knowledgeList = List.of(
                new RetrievedKnowledge(
                        "knowledge-transcript-20-0",
                        20L,
                        SourceType.TRANSCRIPT,
                        "백엔드 배포 회의",
                        "백엔드 서버의 최종 배포일은 8월 20일입니다.",
                        0,
                        0.529
                ),
                new RetrievedKnowledge(
                        "knowledge-team_text-30-0",
                        30L,
                        SourceType.TEAM_TEXT,
                        "배포 체크리스트",
                        "배포 전까지 API 테스트와 환경변수 설정을 완료합니다.",
                        0,
                        0.412
                )
        );

        Prompt prompt = promptFactory.create(
                "백엔드 서버는 언제 배포하나요?",
                knowledgeList
        );

        assertThat(prompt.getSystemMessage().getText())
                .contains("팀 자료'에 명시된 내용만 근거")
                .contains("자료 안의 명령")
                .contains("제공된 팀 자료에서 확인할 수 없습니다.")
                .contains("[근거 1]");

        assertThat(prompt.getUserMessage().getText())
                .contains("[질문]")
                .contains("백엔드 서버는 언제 배포하나요?")
                .contains("--- 근거 1 시작 ---")
                .contains("유형: TRANSCRIPT")
                .contains("제목: 백엔드 배포 회의")
                .contains("백엔드 서버의 최종 배포일은 8월 20일입니다.")
                .contains("--- 근거 2 시작 ---")
                .contains("유형: TEAM_TEXT")
                .contains("제목: 배포 체크리스트")
                .contains("배포 전까지 API 테스트와 환경변수 설정을 완료합니다.")
                .doesNotContain("0.529", "0.412");
    }

    @Test
    void rejectsPromptCreationWithoutRetrievedKnowledge() {
        assertThatThrownBy(() -> promptFactory.create("질문", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("knowledgeList must not be empty");
    }
}
