package com._penLearning.Noddi.global.infrastructure.openAi;

import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.summary.code.SummaryErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    @Qualifier("openAiRestClient")
    private final RestClient openAiRestClient;

    @SuppressWarnings("unchecked")
    public String transcribeAudio(String audioUrl) {
        log.info("[OpenAI Whisper] STT 변환 시작");
        try {
            URL url = new URI(audioUrl).toURL();
            //실제 audio 데이터가 담길 바구니
            byte[] audioBytes;

            try (InputStream in = url.openStream()) {
                audioBytes = in.readAllBytes(); // 빨대를 통해 모든 오디오 바이트 데이터를 RAM 메모리로 쭉 빨아들입니다!
            }
            // 메모리에 있는 바이트 데이터를 "recording.mp4"라는 이름을 가진 '가짜 실물 파일' 객체로 포장합니다.
            ByteArrayResource audioResource = new ByteArrayResource(audioBytes) {
                @Override
                public String getFilename() { return "recording.mp4"; }
            };
            // 4. HTTP POST 요청에 보낼 Body(폼 데이터)를 박스에 차곡차곡 담습니다.
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", audioResource);
            body.add("model", "whisper-1");
            body.add("language", "ko");
            // 5. 드디어 OpenAI 서버로 HTTP 요청을 쏩니다!
            Map<String, Object> response = openAiRestClient.post()
                    .uri("/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            // 응답에서 "text"라는 열쇠(Key)가 있으면, 그 안에 든 원문 문자열을 빼서 리턴
            if (response != null && response.containsKey("text")) {
                return (String) response.get("text");
            }
            throw new GeneralException(SummaryErrorCode.STT_PROCESSING_FAILED); // 만약 "text"가 없으면 에러를 냅니다.
        } catch (Exception e) {
            log.error("[OpenAI Whisper] STT 변환 실패", e);
            throw new GeneralException(SummaryErrorCode.STT_PROCESSING_FAILED);
        }
    }

    @SuppressWarnings("unchecked")
    public String summarizeText(String rawTranscript) {
        log.info("[OpenAI ChatGPT] 회의록 JSON 요약 시작");
        try {
            //프롬포트 작성
            String systemPrompt = "너는 비즈니스 회의 요약 전문가야. 제공되는 회의 대화록을 바탕으로 반드시 완벽한 JSON 형식으로만 응답해.\n\n" +
                    "【JSON 구조 필수 조건】\n" +
                    "{\n" +
                    "  \"summary\": \"회의 전체 핵심 요약 (3~4줄)\",\n" +
                    "  \"decisions\": [\"결정사항 1\", \"결정사항 2\"],\n" +
                    "  \"issues\": [\n" +
                    "    { \"title\": \"이슈 내용\", \"status\": \"미해결/검토 중/완료 중 택 1\" }\n" +
                    "  ],\n" +
                    "  \"tasks\": [\n" +
                    "    { \"assignee\": \"담당자 이름(없으면 미정)\", \"task\": \"할 일 내용\", \"deadline\": \"마감기한(반드시 YYYY-MM-DD 형식. 연도를 모르면 올해 연도 사용. 도저히 파악 불가면 문자열 \\\"null\\\")\" }\n" +
                    "  ]\n" +
                    "}";
            // 시스템 명령(system)과 사용자의 원문(user)을 묶어서 채팅 기록(messages)을 생성
            List<Map<String, String>> messages = List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", "다음 회의 대화록을 JSON으로 요약해줘:\n\n" + rawTranscript)
            );
            // ChatGPT에게 보낼 옵션들을 세팅
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-4o-mini", // 사용할 싸고 빠른 모델
                    "messages", messages,
                    "temperature", 0.3, // 창의성 수치 (낮을수록 보수적이고 정확한 요약을 함)
                    "response_format", Map.of("type", "json_object")
            );
            // OpenAI 챗봇 서버로 요청 (응답 대기 최대 2분)
            Map<String, Object> response = openAiRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
            throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
        } catch (Exception e) {
            log.error("[OpenAI ChatGPT] 요약 실패", e);
            throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
        }
    }
}
