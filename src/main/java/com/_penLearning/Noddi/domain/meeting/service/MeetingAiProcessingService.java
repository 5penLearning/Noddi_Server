package com._penLearning.Noddi.domain.meeting.service;

import com._penLearning.Noddi.domain.meeting.code.AiStatus;
import com._penLearning.Noddi.domain.meeting.code.MeetingErrorCode;
import com._penLearning.Noddi.domain.meeting.entity.Meeting;
import com._penLearning.Noddi.domain.meeting.event.MeetingAiProcessingRequestedEvent;
import com._penLearning.Noddi.domain.meeting.repository.MeetingRepository;
import com._penLearning.Noddi.domain.summary.code.SummaryErrorCode;
import com._penLearning.Noddi.domain.summary.entity.ActionItem;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import com._penLearning.Noddi.domain.summary.repository.ActionItemRepository;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.domain.team.entity.TeamMember;
import com._penLearning.Noddi.domain.team.repository.TeamMemberRepository;
import com._penLearning.Noddi.domain.user.entity.User;
import com._penLearning.Noddi.global.exception.GeneralException;
import com._penLearning.Noddi.global.infrastructure.openAi.OpenAiClient;
import com._penLearning.Noddi.global.infrastructure.openAi.dto.OpenAiRequestDto;
import com._penLearning.Noddi.global.infrastructure.openAi.dto.OpenAiResponseDto;
import com._penLearning.Noddi.global.infrastructure.webRtc.WebRtcClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingAiProcessingService {

    private final MeetingRepository meetingRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TransactionTemplate transactionTemplate;
    private final WebRtcClient webRtcClient;
    private final OpenAiClient openAiClient;
    private final MeetingSummaryRepository meetingSummaryRepository;
    private final ActionItemRepository actionItemRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void processAfterCommit(MeetingAiProcessingRequestedEvent event) {

        Long meetingId = event.meetingId();
        log.info("[MeetingAiProcessingService] AI 회의록 자동 생성 시작: meetingId={}", event.meetingId());

        try{
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

            String recordingAccessLink = webRtcClient.getRecordingAccessLink(context.recordingId());
            log.info(
                    "[MeetingAiProcessingService] 녹음 다운로드 URL 발급 완료: meetingId={}",
                    meetingId
            );

            String rawTranscript = openAiClient.transcribeAudio(recordingAccessLink);
            log.info(
                    "[MeetingAiProcessingService] 회의 음성 전사 완료: meetingId={}, transcriptLength={}",
                    meetingId,
                    rawTranscript.length()
            );

            OpenAiResponseDto.MeetingSummary aiResult = openAiClient.summarizeText(
                    rawTranscript,
                    LocalDate.now(),
                    context.teamMembers()
            );
            int actionItemCount = aiResult.actionItems() != null ? 0 : aiResult.actionItems().size();
            log.info(
                    "[MeetingAiProcessingService] 회의 구조화 요약 완료: meetingId={}, actionItemCount={}",
                    meetingId,
                    actionItemCount
            );
            saveResultAtomically(meetingId, rawTranscript, aiResult);
            log.info(
                    "[MeetingAiProcessingService] AI 회의록 DB 저장 완료: meetingId={}",
                    meetingId
            );

        }catch (Exception e) {
            log.error(
                    "[MeetingAiProcessingService] AI 회의록 자동 생성 실패: meetingId={}",
                    meetingId,
                    e
            );
            markAiProcessingFailed(meetingId);
        }

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

    private void markAiProcessingFailed(Long meetingId) {
        try{
            transactionTemplate.executeWithoutResult(status -> {
                meetingRepository.findByIdWithPessimisticLock(meetingId)
                        .ifPresent(meeting -> {
                            //완료되지 않은 결과만 덮음
                            if (meeting.getAiStatus() == AiStatus.PROCESSING) {
                                meeting.failAiProcessing();
                            }
                        });
            });
        }catch (Exception statusUpdateException) {
            log.error(
                    "[MeetingAiProcessingService] AI 실패 상태 저장 실패: meetingId={}",
                    meetingId,
                    statusUpdateException
            );
        }
    }

    /**
     * AI ActionItem을 DB 엔티티로 변환
     * AI가 반환한 담당자는 현재 팀원 정보와 다시 비교하고,
     * ID와 이름이 모두 정확히 일치할 때만 실제 사용자와 연결한다.
     */
    private ActionItem toActionItem(MeetingSummary meetingSummary, OpenAiResponseDto.ActionItem aiActionItem, Map<Long, User> teamUserById) {
        User matchedUser = teamUserById.get(aiActionItem.assigneeUserId());
        boolean isExactAssignee =
                matchedUser != null
                        && Objects.equals(matchedUser.getName(), aiActionItem.assigneeName())
                        && !aiActionItem.isUncertain();
        User assignee = isExactAssignee ? matchedUser : null;
        boolean isUncertain = !isExactAssignee;

        if(!StringUtils.hasText(aiActionItem.content())) {
            throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
        }

        return ActionItem.builder()
                .meetingSummary(meetingSummary)
                .assignee(assignee)
                .content(aiActionItem.content())
                .isUncertain(isUncertain)
                .dueDate(aiActionItem.dueDate())
                .build();
    }
    //MeetingSummary와 ActionItem을 하나의 트랜잭션에서 저장
    private void saveResultAtomically(Long meetingId, String rawTranscript, OpenAiResponseDto.MeetingSummary aiResult) {
        transactionTemplate.executeWithoutResult(status -> {
            Meeting meeting = meetingRepository.findByIdWithPessimisticLock(meetingId)
                    .orElseThrow(() -> new GeneralException(MeetingErrorCode.MEETING_NOT_FOUND));

            if (meetingSummaryRepository.findByMeeting_MeetingId(meetingId).isPresent()) {
                return;
            }

            if (meeting.getAiStatus() != AiStatus.PROCESSING) {
                throw new GeneralException(SummaryErrorCode.SUMMARY_PROCESSING_FAILED);
            }
            MeetingSummary meetingSummary = MeetingSummary.builder()
                    .meeting(meeting)
                    .summaryText(aiResult.summary())
                    .decisions(aiResult.decisions())
                    .issues(aiResult.issues())
                    .rawTranscript(rawTranscript)
                    .build();
            meetingSummaryRepository.save(meetingSummary);

            Map<Long,User> teamUsersById = new LinkedHashMap<>();

            for (TeamMember teamMember : teamMemberRepository.findAllByTeamWithUser(meeting.getTeam())) {
                User user = teamMember.getUser();
                teamUsersById.put(user.getUserId(), user);
            }
            // AI가 ActionItem 목록을 null로 반환해도 빈 목록으로 저장
            List<OpenAiResponseDto.ActionItem> aiActionItems = aiResult.actionItems() != null ? aiResult.actionItems() : List.of();

            //Ai ActionItem을 검증하면서 엔티티로 변환
            List<ActionItem> actionItems = aiActionItems.stream()
                    .map(aiActionItem -> toActionItem(meetingSummary, aiActionItem, teamUsersById))
                    .toList();
            actionItemRepository.saveAll(actionItems);
            meeting.completeAiProcessing();
        });
    }

    private record ProcessingContext(String recordingId, List<OpenAiRequestDto.TeamMember> teamMembers) {
    }
}
