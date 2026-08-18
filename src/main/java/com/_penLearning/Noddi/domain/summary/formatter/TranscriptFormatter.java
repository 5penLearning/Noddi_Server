package com._penLearning.Noddi.domain.summary.formatter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

/**
 * Whisper 전사 원문의 단어는 변경하지 않고 공백과 문장 경계만 정리한다.
 *
 * DB에는 원문을 그대로 보존하고, 회의록 화면에 응답할 때만 적용한다.
 */
@Component
public class TranscriptFormatter {

    private static final Pattern HORIZONTAL_WHITESPACE =
            Pattern.compile("[\\t\\x0B\\f ]+");

    private static final Pattern LINE_BREAKS =
            Pattern.compile("\\h*\\R+\\h*");

    /**
     * 문장 부호 뒤의 공백을 줄바꿈으로 바꾼다.
     * 닫는 따옴표나 괄호가 문장 부호 뒤에 붙은 경우도 같은 문장으로 유지한다.
     */
    private static final Pattern SENTENCE_BOUNDARY =
            Pattern.compile("([.!?。！？…]+[\\\"'”’)]*)(?:\\h|\\R)+");

    public String format(String transcript) {
        if (!StringUtils.hasText(transcript)) {
            return transcript;
        }

        String normalized = HORIZONTAL_WHITESPACE
                .matcher(transcript.strip())
                .replaceAll(" ");

        normalized = LINE_BREAKS
                .matcher(normalized)
                .replaceAll("\n");

        return SENTENCE_BOUNDARY
                .matcher(normalized)
                .replaceAll("$1\n");
    }
}
