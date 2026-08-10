package com._penLearning.Noddi.global.infrastructure.webRtc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

// Daily.co 웹훅 실제 수신 JSON 페이로드 Dto
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyWebhookPayloadDto {

    // Daily.co는 이벤트 종류를 "type" 필드로
    @JsonProperty("type")
    private String type;

    // 세부 데이터는 "payload" 중첩 객체 안에 있음
    @JsonProperty("payload")
    private EventDetail payload;

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EventDetail {
        @JsonProperty("room")
        private String room;

        @JsonProperty("room_name")
        private String roomName;

        @JsonProperty("recording_id")
        private String recordingId;

        @JsonProperty("s3_key")
        private String s3Key;

        @JsonProperty("s3_bucket")
        private String s3Bucket;

        public String getRoomName() {
            return room != null ? room : roomName;
        }
    }

    // 💡 기존 컨트롤러 코드와의 호환성을 위한 헬퍼 메서드들
    public String getType() {
        return type;
    }



    public String getRoomName() {
        return payload != null ? payload.getRoomName() : null;
    }

    public String getRecordingId() {
        return payload != null ? payload.getRecordingId() : null;
    }

    public String getS3Bucket() {
        return payload != null ? payload.getS3Bucket() : null;
    }

    public String getS3Key() {
        return payload != null ? payload.getS3Key() : null;
    }
}