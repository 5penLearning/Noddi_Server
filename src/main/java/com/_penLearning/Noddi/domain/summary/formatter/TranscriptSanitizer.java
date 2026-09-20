package com._penLearning.Noddi.domain.summary.formatter;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * STT 결과에 섞인 인코딩 흔적과 불필요한 공백을 저장 전에 정리한다.
 *
 * 발음이 비슷한 단어를 임의로 교정하지 않고, 원문의 의미가 변하지 않는
 * 기계적인 정규화만 담당한다.
 */
@Component
public class TranscriptSanitizer {

    private static final Pattern INVISIBLE_CHARACTERS =
            Pattern.compile("[\\u200B-\\u200D\\u2060\\uFEFF]");

    private static final Pattern WHITESPACE =
            Pattern.compile("\\s+");

    public String sanitize(String transcript) {
        if (!StringUtils.hasText(transcript)) {
            return transcript;
        }

        String sanitized = HtmlUtils.htmlUnescape(transcript);
        sanitized = Normalizer.normalize(sanitized, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ');
        sanitized = INVISIBLE_CHARACTERS.matcher(sanitized).replaceAll("");

        return WHITESPACE.matcher(sanitized)
                .replaceAll(" ")
                .trim();
    }
}
