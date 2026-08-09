package com._penLearning.Noddi.domain.meeting.controller;

import com._penLearning.Noddi.domain.meeting.service.MeetingService;
import com._penLearning.Noddi.global.infrastructure.webRtc.dto.DailyWebhookPayloadDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class MeetingWebHookController {

    private final MeetingService meetingService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${daily.webhook.secret:}")
    private String webhookSecret;

    @PostMapping("/daily")
    public ResponseEntity<Void> handleDailyWebhook(
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestHeader(value = "X-Webhook-Timestamp", required = false) String timestamp,
            @RequestBody String rawPayload
    ) {
        try {
            // 시크릿 키가 세팅되어 있다면 무조건 검증을 수행
            if (webhookSecret != null && !webhookSecret.isEmpty()) {
                // 서명이 아예 안 왔거나, 우리가 계산한 해시값과 다르면 해커의 공격으로 간주
                if (signature == null || !isValidSignature(timestamp, rawPayload, signature)) {
                    log.warn("[Daily.co Webhook] 서명 검증 실패. 비정상적인 접근입니다.");
                    // 401 Unauthorized 에러를 반환
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            }

            DailyWebhookPayloadDto payload = objectMapper.readValue(rawPayload, DailyWebhookPayloadDto.class);

            log.info("[Daily.co Webhook] 이벤트 수신: action={}, roomName={}", payload.getAction(), payload.getRoomName());

            if (payload.getAction() != null) {
                switch (payload.getAction()) {
                    case "recording.ready-to-download" -> {
                        String recordingUrl = buildS3Url(payload.getS3Bucket(), payload.getS3Key());
                        log.info("[Daily.co Webhook] 녹음본 준비 완료: url={}", recordingUrl);
                        meetingService.updateRecordingUrl(payload.getRoomName(), recordingUrl);
                    }
                    case "meeting.ended" -> {
                        log.info("[Daily.co Webhook] 회의 자동 종료 처리: roomName={}", payload.getRoomName());
                        meetingService.endMeetingByRoomName(payload.getRoomName());
                    }
                    default ->
                            log.debug("[Daily.co Webhook] 처리하지 않는 이벤트: {}", payload.getAction());
                }
            }
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("[Daily.co Webhook] 웹훅 처리 중 에러 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // HMAC-SHA256 해시 검증 로직
    private boolean isValidSignature(String timestamp, String payload, String providedSignature)
            throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);

        String messageToSign = timestamp + "." + payload;
        byte[] computedHash = mac.doFinal(messageToSign.getBytes(StandardCharsets.UTF_8));
        String computedSignature = Base64.getEncoder().encodeToString(computedHash);

        // 우리가 직접 계산한 결과와, Daily.co가 헤더로 보내준 서명이 똑같은지 비교
        return MessageDigest.isEqual(
                computedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String buildS3Url(String bucket, String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
    }
}