package com._penLearning.Noddi.global.infrastructure.webRtc;

import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.infrastructure.webRtc.dto.DailyCreateRoomResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyCoWebRtcClient implements WebRtcClient{

    @Qualifier("dailyRestClient")
    private final RestClient restClient;

    @Override
    public String createRoom() {
        //방 이름 고유화
        String roomName = "noddi-" + UUID.randomUUID().toString().substring(0, 8);

        //현재 시각 + 10분(600초) 후 Unix Timestamp(초 단위) 계산
        long exp = (System.currentTimeMillis() / 1000) + 600;
        try {
            DailyCreateRoomResponseDto response = restClient.post()
                    .uri("/rooms")
                    .body(Map.of(
                            "name", roomName,
                            "privacy", "public",
                            "properties", Map.of(
                                    "enable_recording", "cloud",
                                    "enable_hand_raising", true,     // 1. 손들기 기능 켜기
                                    "enable_emoji_reactions", true,  // 2. 이모지 리액션 켜기
                                    "enable_chat", true,             // 3. 텍스트 채팅창 켜기
                                    "lang", "ko",                     // 4. 한국어 UI 설정
                                    "max_participants", 10,           // 5. 최대 정원 10명 제한
                                    "exp", exp,                       // 6. 10분 후 방 만료
                                    "eject_at_room_exp", true         // 7. 만료 시 자동 강퇴
                            )
                    ))
                    .retrieve()
                    .body(DailyCreateRoomResponseDto.class);

            log.info("[Daily.co] 방 생성 완료: roomName={}", response.getName());
            return response.getName();
        } catch (Exception e) {
            // 💡 타임아웃이나 Daily.co API 에러 발생 시 예쁜 커스텀 에러로 변환!
            log.error("[Daily.co] 방 생성 통신 실패/타임아웃 발생: {}", e.getMessage());
            throw new GeneralException(MeetingErrorCode.WEBRTC_ROOM_CREATE_FAILED);
        }
    }

    @Override
    public void deleteRoom(String roomName) {
        restClient.delete()
                .uri("/rooms/{roomName}", roomName)
                .retrieve()
                .toBodilessEntity();
        log.info("[Daily.co] 방 삭제 완료: roomName={}", roomName);
    }
}
