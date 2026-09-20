package com._penLearning.Noddi.global.infrastructure.openAi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiIssueExtractionPromptTest {

    @Test
    void keepsProblemsAsIssuesWhileCountermeasuresAreInProgress() {
        assertThat(OpenAiClient.ISSUE_EXTRACTION_RULES)
                .contains("임시 통제")
                .contains("조사 착수")
                .contains("대응 중인 상태")
                .contains("완전히 해결됨")
                .contains("summary에 문제·위험·우려·장애·지연·통제 내용")
                .contains("반드시 다시 점검");
    }
}
