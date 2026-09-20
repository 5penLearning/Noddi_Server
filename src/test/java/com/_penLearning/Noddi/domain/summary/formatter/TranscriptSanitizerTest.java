package com._penLearning.Noddi.domain.summary.formatter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptSanitizerTest {

    private final TranscriptSanitizer transcriptSanitizer =
            new TranscriptSanitizer();

    @Test
    void decodesHtmlWhitespaceAndRemovesInvisibleCharacters() {
        String transcript = "회의를 시작합니다.&#x20;\u200B 다음 안건입니다.&nbsp;  확인해주세요.";

        String sanitized = transcriptSanitizer.sanitize(transcript);

        assertThat(sanitized)
                .isEqualTo("회의를 시작합니다. 다음 안건입니다. 확인해주세요.");
    }

    @Test
    void doesNotChangeRecognizedWords() {
        String transcript = "포토톤 할 때 소고인장과 회의했습니다.";

        String sanitized = transcriptSanitizer.sanitize(transcript);

        assertThat(sanitized)
                .isEqualTo("포토톤 할 때 소고인장과 회의했습니다.");
    }
}
