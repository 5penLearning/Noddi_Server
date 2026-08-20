package com._penLearning.Noddi.domain.summary.formatter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TranscriptFormatterTest {

    private final TranscriptFormatter transcriptFormatter =
            new TranscriptFormatter();

    @Test
    void separatesTranscriptAtSentenceBoundaries() {
        // Whisper가 한 줄로 반환한 여러 문장을 문장 부호 기준으로 나눈다.
        String transcript = "회의를 시작합니다. 상준님이 수정안을 작성해주세요! 일정은 괜찮을까요?";

        String formatted = transcriptFormatter.format(transcript);

        assertThat(formatted).isEqualTo("""
                회의를 시작합니다.
                상준님이 수정안을 작성해주세요!
                일정은 괜찮을까요?""");
    }

    @Test
    void keepsClosingQuoteWithPreviousSentenceAndPreservesDecimal() {
        // 닫는 따옴표는 앞 문장에 남기고, 공백이 없는 소수점은 문장 경계로 보지 않는다.
        String transcript = "예산은 3.14억입니다. \"금요일까지 완료합니다.\" 다음 안건입니다.";

        String formatted = transcriptFormatter.format(transcript);

        assertThat(formatted).isEqualTo("""
                예산은 3.14억입니다.
                "금요일까지 완료합니다."
                다음 안건입니다.""");
    }

    @Test
    void normalizesRepeatedSpacesAndExistingLineBreaks() {
        // 연속된 가로 공백은 하나로 줄이고 기존 줄바꿈은 유지한다.
        String transcript = "  첫 번째   문장입니다.  \r\n\r\n 두 번째 문장입니다.  ";

        String formatted = transcriptFormatter.format(transcript);

        assertThat(formatted).isEqualTo("""
                첫 번째 문장입니다.
                두 번째 문장입니다.""");
    }

    @Test
    void returnsBlankTranscriptWithoutModification() {
        // 내용이 없는 입력은 임의의 문자열로 변환하지 않는다.
        assertThat(transcriptFormatter.format("   ")).isEqualTo("   ");
        assertThat(transcriptFormatter.format(null)).isNull();
    }
}
