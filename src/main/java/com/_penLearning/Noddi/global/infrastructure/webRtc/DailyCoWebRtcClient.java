package com._penLearning.Noddi.global.infrastructure.webRtc;

import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.infrastructure.webRtc.dto.DailyAccessLinkResponseDto;
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
                                    "enable_recording", "cloud-audio-only", // 1. 클라우드 오디오 전용 녹음
                                    "enable_hand_raising", true,     // 2. 손들기 기능 켜기
                                    "enable_emoji_reactions", true,  // 3. 이모지 리액션 켜기
                                    "enable_chat", true,             // 4. 텍스트 채팅창 켜기
                                    "lang", "ko",                     // 5. 한국어 UI 설정
                                    "max_participants", 10,           // 6. 최대 정원 10명 제한
                                    "exp", exp,                       // 7. 10분 후 방 만료
                                    "eject_at_room_exp", true         // 8. 만료 시 자동 강퇴
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

    /**
     * Daily.co 녹음 ID로 1시간 유효한 S3 서명 다운로드 URL(download_url) 가져오기
     */
    public String getRecordingAccessLink(String recordingId) {
        try {
            DailyAccessLinkResponseDto response = restClient.get()
                    .uri("/recordings/{recordingId}/access-link", recordingId)
                    .retrieve()
                    .body(DailyAccessLinkResponseDto.class);

            if (response != null && response.getDownloadLink() != null) {
                return (String) response.getDownloadLink();
            }
            throw new GeneralException(MeetingErrorCode.RECORDING_NOT_READY);
        } catch (Exception e) {
            log.error("[Daily.co] 녹음본 다운로드 링크 조회 실패: recordingId={}", recordingId, e);
            throw new GeneralException(MeetingErrorCode.RECORDING_NOT_READY);
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
