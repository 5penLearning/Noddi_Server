package com._penLearning.Noddi.global.infrastructure.openAi;

import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.summary.code.SummaryErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.infrastructure.openAi.dto.OpenApiResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OpenAiClient {

    // 다운로드할 때 한 번에 읽고 쓰는 데이터 크기다
    private static final int DOWNLOAD_BUFFER_SIZE = 8 * 1024;
    private final RestClient openAiRestClient;
    // OpenAI의 파일 전사 업로드 상한
    private final long maxAudioFileSizeBytes;
    // Daily 녹음 파일 서버와 TCP 연결이 만들어질 때까지 기다릴 최대 시간
    private final int audioDownloadConnectTimeoutMs;
    // 연결 이후 파일 데이터가 들어오지 않을 때 기다릴 최대 시간
    private final int audioDownloadReadTimeoutMs;

    public OpenAiClient(
            @Qualifier("openAiRestClient")
            RestClient openAiRestClient,
            // 기본값은 OpenAI 공식 파일 전사 제한인 25,000,000 bytes
            @Value("${openai.audio.max-file-size-bytes:25000000}")
            long maxAudioFileSizeBytes,
            // 다운로드 서버 연결이 10초 동안 만들어지지 않으면 실패 처리
            @Value("${openai.audio.download-connect-timeout-ms:10000}")
            int audioDownloadConnectTimeoutMs,
            // 다운로드 도중 60초 동안 데이터가 들어오지 않으면 실패 처리
            @Value("${openai.audio.download-read-timeout-ms:60000}")
            int audioDownloadReadTimeoutMs
    ) {
        this.openAiRestClient = openAiRestClient;
        this.maxAudioFileSizeBytes = maxAudioFileSizeBytes;
        this.audioDownloadConnectTimeoutMs = audioDownloadConnectTimeoutMs;
        this.audioDownloadReadTimeoutMs = audioDownloadReadTimeoutMs;
    }

    public String transcribeAudio(String audioUrl) {
        log.info("[OpenAI Whisper] STT 변환 시작");

        Path temporaryAudioFile = null;

        try {
            // 1단계: Daily 다운로드 URL의 녹음 파일을 서버의 임시 디렉터리에 스트리밍 방식으로 저장
            temporaryAudioFile = downloadAudioToTemporaryFile(audioUrl);
            // 2단계: 저장된 임시 파일을 multipart/form-data 요청으로 Whisper API에 전달
            return requestWhisperTranscription(temporaryAudioFile);
        } catch (Exception e) {
            log.error("[OpenAI Whisper] STT 변환 실패", e);
            throw new GeneralException(SummaryErrorCode.STT_PROCESSING_FAILED);
        } finally {
            // 성공과 실패 여부에 상관없이 다운로드한 녹음 파일을 서버 디스크에서 삭제
            deleteTemporaryFile(temporaryAudioFile);
        }
    }

    private Path downloadAudioToTemporaryFile(String audioUrl) throws Exception {
        URI audioUri = new URI(audioUrl);

        // 외부에서 전달된 file:// 등의 주소로 서버 내부 파일을 읽지 못하도록 HTTPS 주소만 허용
        if (!"https".equalsIgnoreCase(audioUri.getScheme())) {
            throw new IOException("녹음 파일 다운로드 URL은 HTTPS 형식이어야 합니다.");
        }

        URLConnection connection = audioUri.toURL().openConnection();
        connection.setConnectTimeout(audioDownloadConnectTimeoutMs);
        connection.setReadTimeout(audioDownloadReadTimeoutMs);

        // 서버가 Content-Length를 제공했다면 실제 다운로드 전에 파일이 너무 큰지 빠르게 확인
        long declaredFileSize = connection.getContentLengthLong();
        if (declaredFileSize > maxAudioFileSizeBytes) {
            throw new IOException("녹음 파일이 허용 크기를 초과했습니다. size=" + declaredFileSize);
        }

        // 운영체제의 임시 디렉터리에 저장될 파일을 만든다.
        Path temporaryFile = Files.createTempFile("noddi-recording-", ".mp4");
        // 다운로드 도중 실패한 경우 불완전한 임시 파일을 삭제하기 위한 완료 여부 표시
        boolean downloadCompleted = false;

        try (
                InputStream inputStream = connection.getInputStream();
                OutputStream outputStream = Files.newOutputStream(temporaryFile)
        ) {
            // 매 반복마다 최대 8KB만 메모리에 올라오는 작은 버퍼
            byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            long downloadedBytes = 0L;
            int bytesRead;

            // 파일 끝에 도달하면 read()가 -1을 반환하므로 그때 반복을 종료
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                // 현재까지 받은 크기에 이번에 받은 크기를 더한다.
                downloadedBytes += bytesRead;
                // 실제 다운로드 크기가 제한을 넘는 즉시 중단해 디스크와 OpenAI 업로드를 보호한다.
                if (downloadedBytes > maxAudioFileSizeBytes) {
                    throw new IOException("녹음 파일이 허용 크기를 초과했습니다. size>" + maxAudioFileSizeBytes);
                }
                outputStream.write(buffer, 0, bytesRead);
            }
            downloadCompleted = true;
            return temporaryFile;
        } finally {
            // 다운로드가 중간에 실패했다면 호출자에게 경로가 반환되지 않으므로 여기서 직접 삭제
            if (!downloadCompleted) {
                Files.deleteIfExists(temporaryFile);
            }
        }
    }

    private String requestWhisperTranscription(Path audioFile) {
        // FileSystemResource는 디스크 파일을 multipart의 file 항목으로 전달할 수 있게 포장
        FileSystemResource audioResource = new FileSystemResource(audioFile);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", audioResource);
        body.add("model", "whisper-1");
        body.add("language", "ko");

        OpenApiResponseDto.Transcription response = openAiRestClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(OpenApiResponseDto.Transcription.class);

        if (response != null && StringUtils.hasText(response.text())) {
            return response.text();
        }

        throw new GeneralException(SummaryErrorCode.STT_PROCESSING_FAILED);
    }

    private void deleteTemporaryFile(Path temporaryAudioFile) {
        // 다운로드 전에 실패해 파일이 생성되지 않았다면 삭제할 것이 없으므로 바로 종료
        if (temporaryAudioFile == null) {
            return;
        }

        try {
            // 파일이 존재할 때만 삭제하고, 이미 없어진 경우에는 정상적으로 진행
            Files.deleteIfExists(temporaryAudioFile);
        } catch (IOException e) {
            // 전사는 끝났으므로 삭제 실패가 전체 STT 결과를 실패시키지는 않게 하고 로그로 추적
            log.warn("[OpenAI Whisper] 임시 녹음 파일 삭제 실패. path={}", temporaryAudioFile, e);
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

    private Map<String, Object> createTaskSchema() {
        return Map.of(
                // tasks 배열의 각 항목은 content, 담당자, 마감일을 가진 객체
                "type", "object",

                "properties", Map.of(
                        "content", Map.of(
                                "type", "string",
                                "description", "회의 이후 수행해야 할 구체적인 할 일"
                        ),
                        "assigneeUserId", Map.of(
                                "type", List.of("integer", "null"),
                                "description", "팀원 목록과 정확히 일치하는 담당자 ID"
                        ),
                        "assigneeName", Map.of(
                                "type", List.of("string", "null"),
                                "description", "전문에서 확인된 담당자 이름"
                        ),
                        "dueDate", Map.of(
                                "type", List.of("string", "null"),
                                "description", "YYYY-MM-DD 형식의 마감일"
                        ),
                        "isUncertain", Map.of(
                                "type", "boolean",
                                "description", "담당자 연결이 불확실하면 true"
                        )
                ),

                "required", List.of(
                        "content",
                        "assigneeUserId",
                        "assigneeName",
                        "dueDate",
                        "isUncertain"
                ),

                "additionalProperties", false
        );
    }

    /**
     * OpenAI가 반환할 전체 회의록의 JSON 구조를 정의한다.
     * 최종 응답 DTO인 OpenApiResponseDto.MeetingSummary 구조와 동일하게 맞춘다.
     */
    private Map<String, Object> createMeetingSummarySchema() {
        return Map.of(
                // 전체 회의록 응답은 summary, decisions, issues, tasks를 가진 객체
                "type", "object",

                // 회의록 응답에 들어갈 수 있는 네 가지 필드를 정의
                "properties", Map.of(
                        "summary", Map.of(
                                "type", "string",
                                "description", "회의 전체 내용을 요약한 3~4문장"
                        ),
                        "decisions", Map.of(
                                "type", "array",
                                "description", "회의에서 확정된 결정사항 목록",
                                "items", Map.of("type", "string")
                        ),
                        "issues", Map.of(
                                "type", "array",
                                "description", "회의에서 논의된 문제나 추가 검토 내용",
                                "items", Map.of("type", "string")
                        ),
                        "tasks", Map.of(
                                "type", "array",
                                "description", "회의 이후 수행해야 할 ActionItem 목록",
                                "items", createTaskSchema()
                        )
                ),

                "required", List.of(
                        "summary",
                        "decisions",
                        "issues",
                        "tasks"
                ),

                "additionalProperties", false
        );
    }
}
