package com._penLearning.Noddi.global.infrastructure.webRtc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

//daily.co가 웹훅으로 보내주는 이벤트 페이로드 Dto
@Getter
public class DailyWebhookPayloadDto {
    //(예: "recording.ready", "meeting.ended")
    private String action;

    @JsonProperty("room_name")
    private String roomName;

    //recording.ready 이벤트 시에만 채워짐
    @JsonProperty("recording_id")
    private String recordingId;

    @JsonProperty("s3_key")
    private String s3Key;

    @JsonProperty("s3_bucket")
    private String s3Bucket;
}
