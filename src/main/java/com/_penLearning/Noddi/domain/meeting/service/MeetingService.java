package com._penLearning.Noddi.domain.meeting.service;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.meeting.code.MeetingStatus;
import com._penLearning.Noddi.domain.meeting.dto.MeetingRequestDto;
import com._penLearning.Noddi.domain.meeting.dto.MeetingResponseDto;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.entity.MeetingParticipant;
import com._penLearning.Noddi.domain.meeting.repository.MeetingParticipantRepository;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.team.entity.Team;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.team.repository.TeamRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.domain.user.repository.UserRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.infrastructure.webRtc.WebRtcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final WebRtcClient webRtcClient;
    private final TransactionTemplate transactionTemplate;

    public MeetingResponseDto.Info getMeetingInfo(Long meetingId, Long currentUserId) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        User user = getUserOrThrow(currentUserId);
        validateTeamMember(meeting.getTeam(), user);
        return MeetingResponseDto.Info.from(meeting);
    }

    public List<MeetingResponseDto.Info> getMeetingsByTeam(Long teamId, Long currentUserId) {
        Team team = getTeamOrThrow(teamId);
        User user = getUserOrThrow(currentUserId);
        validateTeamMember(team, user);
        return meetingRepository.findAllByTeam(team).stream()
                .map(MeetingResponseDto.Info::from)
                .toList();
    }

    public List<MeetingResponseDto.ParticipantInfo> getParticipants(Long meetingId, Long currentUserId) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        User user = getUserOrThrow(currentUserId);
        validateTeamMember(meeting.getTeam(), user);
        return meetingParticipantRepository.findAllByMeeting(meeting).stream()
                .map(MeetingResponseDto.ParticipantInfo::from)
                .toList();
    }
    //회의 예약 생성 (SCHEDULED)
    @Transactional
    public MeetingResponseDto.Info createMeeting(MeetingRequestDto.Create request, Long currentUserId) {
        Team team = getTeamOrThrow(request.getTeamId());
        User user = getUserOrThrow(currentUserId);
        validateTeamMember(team, user);

        Meeting meeting = Meeting.builder()
                .team(team)
                .title(request.getTitle())
                .createdBy(user)
                .build();
        Meeting savedMeeting = meetingRepository.save(meeting);
        log.info("[MeetingService] 회의 예약 생성 완료: meetingId={}",
                savedMeeting.getMeetingId());
        return MeetingResponseDto.Info.from(savedMeeting);
    }

    public MeetingResponseDto.Start startMeeting(Long meetingId, Long currentUserId) {
        Meeting meeting = getMeetingOrThrow(meetingId);
        User user = getUserOrThrow(currentUserId);
        validateTeamMember(meeting.getTeam(), user);

        if(meeting.getStatus() == MeetingStatus.IN_PROGRESS) {
            log.info("[MeetingService] 이미 진행 중인 회의 재입장: meetingId={}", meetingId);
            return transactionTemplate.execute(status -> {
                saveParticipantIfabsent(meeting, user);
                return MeetingResponseDto.Start.from(meeting);
            });
        }

        //트랜잭션 없는 상태에서 외부 API 호출
        String roomName = webRtcClient.createRoom();
        log.info("[MeetingService] Daily.co 방 생성 완료: meetingId={}, roomName={}", meetingId, roomName);
        //DB 트랜잭션을 열고 상태를 바꿈
        try {
            return transactionTemplate.execute(status -> {
                Meeting lockedMeeting = getMeetingWithLockOrThrow(meetingId);

                if (lockedMeeting.getStatus() == MeetingStatus.IN_PROGRESS) {
                    saveParticipantIfabsent(lockedMeeting, user);
                    return MeetingResponseDto.Start.from(lockedMeeting);
                }

                lockedMeeting.start(roomName);
                saveParticipantIfabsent(lockedMeeting, user);
                log.info("[MeetingService] 회의 시작 완료(DB 업데이트): meetingId={}, roomName={}", meetingId, roomName);
                return MeetingResponseDto.Start.from(lockedMeeting);
            });
        }catch (Exception e) {
            log.error("[MeetingService] DB 상태 변경 실패로 인해 Daily.co 방 보상 삭제(롤백) 진행: roomName={}", roomName, e);
            try {
                webRtcClient.deleteRoom(roomName);
            } catch (Exception ex) {
                log.error("[MeetingService] Daily.co 보상 삭제 실패 (수동 확인 필요): roomName={}", roomName, ex);
            }
            throw e;
        }
    }

    public void endMeeting(Long meetingId, Long currentUserId) {

        String roomNameToDelete = transactionTemplate.execute(status -> {
            Meeting meeting = getMeetingWithLockOrThrow(meetingId);
            User user = getUserOrThrow(currentUserId);
            validateTeamMember(meeting.getTeam(), user);

            if (!meeting.getCreatedBy().getUserId().equals(user.getUserId())) {
                throw new GeneralException(MeetingErrorCode.NOT_MEETING_CREATOR);
            }

            meeting.end();
            log.info("[MeetingService] 회의 종료 완료(DB 업데이트): meetingId={}", meetingId);

            return meeting.getRoomName();
        });

        if(roomNameToDelete != null) {
            webRtcClient.deleteRoom(roomNameToDelete);
            log.info("[MeetingService] Daily.co 방 삭제 완료: roomName={}", roomNameToDelete);
        }
    }

    @Transactional
    public void endMeetingByRoomName(String roomName) {
        Meeting meeting = getMeetingByRoomNameOrThrow(roomName);
        // 이미 종료된 회의가 아닐 때만 종료 처리
        if (meeting.getStatus() == MeetingStatus.IN_PROGRESS) {
            meeting.end();
            log.info("[MeetingService] Webhook에 의해 회의 자동 종료 완료: roomName={}", roomName);
        }
    }

    //웹훅 수신: 녹음본 S3 업로드 완료 시 URL 갱신
    @Transactional
    public void updateRecordingUrl(String roomName, String recordingUrl) {
        Meeting meeting = getMeetingByRoomNameOrThrow(roomName);
        meeting.updateRecordingUrl(recordingUrl);
        log.info("[MeetingService] Webhook 녹음본 URL 갱신 완료: meetingId={}, url={}",
                meeting.getMeetingId(), recordingUrl);
    }

    @Transactional
    public void triggerSummary(Long meetingId, Long currentUserId) {
        Meeting meeting = getMeetingWithLockOrThrow(meetingId);
        User user = getUserOrThrow(currentUserId);
        validateTeamMember(meeting.getTeam(), user);

        //  ENDED(종료된) 회의만 요약 가능
        if (meeting.getStatus() != MeetingStatus.ENDED) {
            throw new GeneralException(MeetingErrorCode.INVALID_STATUS_FOR_SUMMARY);
        }
        // 이미 PROCESSING(요약 중)이거나 COMPLETED(완료)면 중복 연타 거부
        if (meeting.getAiStatus() == AiStatus.PROCESSING || meeting.getAiStatus() == AiStatus.COMPLETED) {
            throw new GeneralException(MeetingErrorCode.ALREADY_PROCESSING_SUMMARY);
        }

        if (meeting.getRecordingUrl() == null) {
            throw new GeneralException(MeetingErrorCode.RECORDING_NOT_READY);
        }
        meeting.startAiProcessing();
        log.info("[MeetingService] AI 요약 요청 수신 완료: meetingId={}", meetingId);
    }

    private void validateTeamMember(Team team, User user) {
        boolean isMember = teamMemberRepository.existsByTeamAndUser(team, user);
        if (!isMember) {
            throw new GeneralException(MeetingErrorCode.NOT_TEAM_MEMBER);
        }
    }

    private Team getTeamOrThrow(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new GeneralException(MeetingErrorCode.TEAM_NOT_FOUND));
    }
    private Meeting getMeetingOrThrow(Long meetingId) {
        return meetingRepository.findById(meetingId)
                .orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
    }

    private Meeting getMeetingByRoomNameOrThrow(String roomName) {
        return meetingRepository.findByRoomName(roomName)
                .orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(MeetingErrorCode.USER_NOT_FOUND));
    }

    private Meeting getMeetingWithLockOrThrow(Long meetingId) {
        return meetingRepository.findByIdWithPessimisticLock(meetingId)
                .orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));
    }

    private void saveParticipantIfabsent(Meeting meeting, User user) {
        boolean exists = meetingParticipantRepository.findByMeetingAndUser(meeting, user).isPresent();
        if (!exists) {
            MeetingParticipant participant = MeetingParticipant.builder()
                    .meeting(meeting)
                    .user(user)
                    .build();
            meetingParticipantRepository.save(participant);
        }
    }
}
