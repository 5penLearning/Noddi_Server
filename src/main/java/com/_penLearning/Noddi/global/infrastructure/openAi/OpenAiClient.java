package com._penLearning.Noddi.global.infrastructure.openAi;

import com._penLearning.Noddi.domain.summary.code.SummaryErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.infrastructure.openAi.dto.OpenAiRequestDto;
import com._penLearning.Noddi.global.infrastructure.openAi.dto.OpenAiResponseDto;
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
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
    private final ObjectMapper objectMapper;

    public OpenAiClient(
            @Qualifier("openAiRestClient")
            RestClient openAiRestClient,
            ObjectMapper objectMapper,
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
        this.objectMapper = objectMapper;
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

        OpenAiResponseDto.Transcription response = openAiRestClient.post()
                .uri("/audio/transcriptions")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .body(OpenAiResponseDto.Transcription.class);

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

    public OpenAiResponseDto.MeetingSummary summarizeText(
            String rawTranscript,
            LocalDate currentDate,
            List<OpenAiRequestDto.TeamMember> teamMembers
    ) {
        log.info("[OpenAI ChatGPT] 회의록 구조화 요약 시작");

        try {
            // 전문이 비어 있으면 OpenAI를 불필요하게 호출하지 않고 즉시 실패 처리
            if (!StringUtils.hasText(rawTranscript)) {
                throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
            }

            // 팀원이 없는 경우에도 프롬프트에는 null이 아닌 빈 JSON 배열([])을 전달
            List<OpenAiRequestDto.TeamMember> safeTeamMembers =
                    teamMembers == null ? List.of() : teamMembers;

            // TeamMember DTO 목록을 AI가 정확히 읽을 수 있는 JSON 문자열로 변환
            String teamMembersJson = objectMapper.writeValueAsString(safeTeamMembers);

            // system 메시지에는 요약 기준과 담당자·마감일 판단 규칙을 정의
            String systemPrompt = """
                    너는 한국어 비즈니스 회의록 작성 전문가다.

                    회의 전문을 근거로 다음 내용을 추출한다.
                    - 회의 전체 요약
                    - 확정된 결정사항
                    - 논의된 문제 또는 추가 검토 이슈
                    - 회의 중 특정 팀원이 회의 이후 수행하기로 약속했거나 요청받은 구체적인 업무(ActionItem)

                    ActionItem 추출 규칙:
                    - ActionItem은 회의 이후 누군가가 실제로 수행해야 하는 구체적인 업무다.
                    - 참석자가 직접 수행하겠다고 약속한 업무와 다른 사람에게 명시적으로 요청하거나 배정한 업무만 추출한다.
                    - 단순한 의견, 아이디어, 제안, 질문, 정보 공유, 논의 주제는 ActionItem으로 추출하지 않는다.
                    - 회의 전에 이미 완료된 업무나 전문에서 완료되었다고 보고한 업무는 ActionItem으로 추출하지 않는다.
                    - 동일한 업무가 여러 번 언급되면 하나의 ActionItem으로 통합한다.
                    - content에는 회의 내용을 그대로 나열하지 말고, 수행할 행동과 결과물이 드러나는 구체적인 문장 하나를 작성한다.
                    - 추출한 각 업무는 응답의 actionItems 배열에 담는다.
                    - 수행할 업무가 명확하지 않다면 억지로 만들지 말고 actionItems를 빈 배열로 반환한다.

                    담당자 연결 규칙:
                    - 전문에서 담당자가 명시된 경우에만 담당자를 연결한다.
                    - 제공된 팀원 목록의 userId와 name만 담당자 후보로 사용한다.
                    - 이름과 사용자가 정확히 한 명의 팀원과 일치할 때만 assigneeUserId와 assigneeName을 반환한다.
                    - 담당자가 불명확하거나 동명이인이거나 팀 외 사용자라면 assigneeUserId와 assigneeName은 null이다.
                    - 담당자를 확정할 수 없으면 isUncertain은 true, 확정할 수 있으면 false다.
                    - 화자 구분이 없는 전문에서 '제가 하겠습니다' 같은 표현만으로 담당자를 추측하지 않는다.

                    마감일 규칙:
                    - 제공된 현재 날짜를 기준으로 상대적인 날짜를 YYYY-MM-DD로 변환한다.
                    - 전문에서 마감일을 확인할 수 없으면 dueDate는 null이다.
                    - 전문에 없는 담당자, 날짜, 결정사항을 만들거나 추측하지 않는다.
                    """;

            // user 메시지에는 실제 분석에 필요한 기준 날짜, 팀원 목록, 회의 전문을 전달
            String userPrompt = """
                    현재 날짜:
                    %s

                    팀원 목록:
                    %s

                    회의 전문:
                    %s
                    """.formatted(
                    currentDate,
                    teamMembersJson,
                    rawTranscript
            );

            // OpenAI의 system/user 메시지를 Map이 아닌 요청 DTO로 구성
            List<OpenAiRequestDto.Message> messages = List.of(
                    new OpenAiRequestDto.Message("system", systemPrompt),
                    new OpenAiRequestDto.Message("user", userPrompt)
            );

            // 앞에서 정의한 회의록 Schema에 이름을 붙이고 strict 모드를 활성화
            OpenAiRequestDto.JsonSchema jsonSchema =
                    new OpenAiRequestDto.JsonSchema(
                            "meeting_summary",
                            true,
                            createMeetingSummarySchema()
                    );

            // 일반 JSON 모드가 아니라 JSON Schema 기반 Structured Outputs를 요청
            OpenAiRequestDto.ResponseFormat responseFormat =
                    new OpenAiRequestDto.ResponseFormat(
                            "json_schema",
                            jsonSchema
                    );

            // OpenAI Chat Completions API에 전달할 최상위 요청 DTO를 완성
            OpenAiRequestDto.ChatCompletion request =
                    new OpenAiRequestDto.ChatCompletion(
                            "gpt-4o-mini",
                            messages,
                            0.2,
                            responseFormat
                    );

            // 응답 JSON을 Map으로 받지 않고 ChatCompletion DTO로 바로 역직렬화한다.
            OpenAiResponseDto.ChatCompletion response = openAiRestClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(OpenAiResponseDto.ChatCompletion.class);

            // 응답 또는 choices가 비어 있으면 정상적인 요약 결과가 아니므로 실패 처리한다.
            if (response == null
                    || response.choices() == null
                    || response.choices().isEmpty()) {
                throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
            }

            // 현재는 OpenAI가 반환한 첫 번째 응답 후보를 사용한다.
            OpenAiResponseDto.Message message = response.choices().getFirst().message();

            // message가 없거나 안전 정책에 따른 refusal이 있으면 저장 가능한 결과가 아니다.
            if (message == null || StringUtils.hasText(message.refusal())) {
                throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
            }

            // Structured Outputs 결과는 message.content 안에 JSON 문자열 형태로 들어 있다.
            if (!StringUtils.hasText(message.content())) {
                throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
            }

            // content JSON 문자열을 최종 MeetingSummary DTO로 변환해 호출자에게 반환한다.
            return objectMapper.readValue(
                    message.content(),
                    OpenAiResponseDto.MeetingSummary.class
            );
        } catch (GeneralException e) {
            // 이미 프로젝트 공통 예외로 판단한 오류는 다른 예외로 다시 감싸지 않고 그대로 전달한다.
            throw e;
        } catch (Exception e) {
            // 네트워크, 요청 직렬화, 응답 JSON 변환 오류를 회의록 요약 실패로 통일한다.
            log.error("[OpenAI ChatGPT] 구조화 요약 실패", e);
            throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
        }
    }

    private Map<String, Object> createActionItemSchema() {
        return Map.of(
                // actionItems 배열의 각 항목은 content, 담당자, 마감일을 가진 객체
                "type", "object",

                "properties", Map.of(
                        "content", Map.of(
                                "type", "string",
                                "description", "회의 중 누군가가 수행하기로 약속했거나 명시적으로 요청·배정받은, 행동과 결과물이 드러나는 구체적인 업무"
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
                // 전체 회의록 응답은 summary, decisions, issues, actionItems를 가진 객체
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
                        "actionItems", Map.of(
                                "type", "array",
                                "description", "회의 이후 누군가가 실제로 수행하기로 약속했거나 명시적으로 요청·배정받은 업무 목록. 단순 의견, 아이디어, 질문, 정보 공유 및 이미 완료된 업무는 제외한다.",
                                "items", createActionItemSchema()
                        )
                ),

                "required", List.of(
                        "summary",
                        "decisions",
                        "issues",
                        "actionItems"
                ),

                "additionalProperties", false
        );
    }
}
