package com._penLearning.Noddi.domain.meeting.controller;

import com._penLearning.Noddi.domain.meeting.service.MeetingService;
import com._penLearning.Noddi.global.infrastructure.webRtc.dto.DailyWebhookPayloadDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//비동기 요청 확인 위한 로그 출력 목적 태그
@Slf4j
@RestController
@RequestMapping("/webhooks")
@RequiredArgsConstructor
public class MeetingWebHookController {
    private final MeetingService meetingService;

    @PostMapping("/daily")
    public ResponseEntity<Void> handleDailyWebhook(
            @RequestBody DailyWebhookPayloadDto payload
    ) {
        log.info("[Daily.co Webhook] 이벤트 수신: action={}, roomName={}",
                payload.getAction(), payload.getRoomName());
        switch (payload.getAction()) {
            case "recording.ready" -> {
                String recordingUrl = buildS3Url(payload.getS3Bucket(), payload.getS3Key());
                log.info("[Daily.co Webhook] 녹음본 준비 완료: url={}", recordingUrl);
                meetingService.updateRecordingUrl(payload.getRoomName(), recordingUrl);
            }
            case "meeting.ended" -> {
                log.info("[Daily.co Webhook] 회의 자동 종료 처리: roomName={}", payload.getRoomName());
                //자동 종료 메서드 추가
                 meetingService.endMeetingByRoomName(payload.getRoomName());
            }
            default ->
                    log.debug("[Daily.co Webhook] 처리하지 않는 이벤트: {}", payload.getAction());
        }
        return ResponseEntity.ok().build();
    }
    private String buildS3Url(String bucket, String key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucket, key);
    }

}
