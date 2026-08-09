package com._penLearning.Noddi.global.infrastructure.webRtc;

//Api변경 확장성을 위해 인터페이스를 사용합니다
public interface WebRtcClient {
    /**
     * Daily.co에 방을 생성하고 roomId(URL)를 반환합니다.
     * @return 생성된 방의 고유 이름 (예: "noddi-room-abc123")
     */
    String createRoom();
    /**
     * Daily.co에 방을 삭제합니다. (회의 종료 시 호출)
     * @param roomName 삭제할 방 이름
     */
    void deleteRoom(String roomName);
}
