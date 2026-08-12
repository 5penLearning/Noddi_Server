package com._penLearning.Noddi.domain.meeting.service;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.event.MeetingAiProcessingRequestedEvent;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.global.infrastructure.openAi.dto.OpenAiRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingAiProcessingService {

    private final MeetingRepository meetingRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TransactionTemplate transactionTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAfterCommit(MeetingAiProcessingRequestedEvent event) {

        log.info("[MeetingAiProcessingService] AI 회의록 자동 생성 시작: meetingId={}", event.meetingId());
        ProcessingContext context = loadProcessingContext(event.meetingId());

        if (context == null) {
            return;
        }

        log.info(
                "[MeetingAiProcessingService] AI 처리 데이터 조회 완료: meetingId={}, recordingId={}, teamMemberCount={}",
                event.meetingId(),
                context.recordingId(),
                context.teamMembers().size()
        );
    }

    private ProcessingContext loadProcessingContext(Long meetingId)
    {
        return transactionTemplate.execute(status -> {
            Meeting meeting = meetingRepository.findById(meetingId)
                    .orElse(null);

            // 이벤트 실행 시점에 회의가 삭제된 경우 작업을 중단
            if (meeting == null) {
                log.warn("[MeetingAiProcessingService] 회의를 찾을 수 없어 AI 처리를 중단합니다: meetingId={}", meetingId);
                return null;
            }

            // 중복되거나 오래된 이벤트라면 외부 API를 호출하지 않는다.
            if (meeting.getAiStatus() != AiStatus.PROCESSING) {
                log.info("[MeetingAiProcessingService] AI 처리 대상 상태가 아닙니다: meetingId={}, aiStatus={}", meetingId, meeting.getAiStatus());
                return null;
            }

            // 해당 회의 참석자가 아니라 회의가 속한 팀의 전체 팀원을 전달
            List<OpenAiRequestDto.TeamMember> teamMembers =
                    teamMemberRepository
                            .findAllByTeamWithUser(meeting.getTeam())
                            .stream()
                            .map(teamMember ->
                                    new OpenAiRequestDto.TeamMember(
                                            teamMember.getUser().getUserId(),
                                            teamMember.getUser().getName()
                                    )
                            )
                            .toList();

            return new ProcessingContext(
                    meeting.getRecordingId(),
                    teamMembers
            );
        });
    }

    private record ProcessingContext(String recordingId, List<OpenAiRequestDto.TeamMember> teamMembers) {
    }

}
