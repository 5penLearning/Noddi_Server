package com._penLearning.Noddi.global.infrastructure.webRtc.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

//daily.co REST API의 방 생성 응답 JSON을 매핑하는 Dto
@Getter
public class DailyCreateRoomResponseDto {
    private String name;
}
