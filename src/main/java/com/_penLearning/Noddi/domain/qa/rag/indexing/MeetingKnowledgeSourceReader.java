package com._penLearning.Noddi.domain.qa.rag.indexing;

import com._penLearning.Noddi.domain.qa.entity.SourceType;
import com._penLearning.Noddi.domain.summary.code.SummaryErrorCode;
import com._penLearning.Noddi.domain.summary.entity.MeetingSummary;
import com._penLearning.Noddi.domain.summary.repository.MeetingSummaryRepository;
import com._penLearning.Noddi.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// DB의 MeetingSummary에서 RAG에 필요한 전사 원문과 회의 정보를 읽어옴
// 읽어온 값은 공통 자료형은 KnowledgeSourceContent로 변환함
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingKnowledgeSourceReader {

    private final MeetingSummaryRepository meetingSummaryRepository;

    public KnowledgeSourceContent read(Long meetingId) {
        MeetingSummary summary = meetingSummaryRepository.findByMeetingIdWithMeetingAndTeam(meetingId)
                .orElseThrow(() -> new GeneralException(SummaryErrorCode.STT_PROCESSING_FAILED));

        return new KnowledgeSourceContent(
                summary.getMeeting().getMeetingId(),
                summary.getMeeting().getTeam().getTeamId(),
                SourceType.TRANSCRIPT,
                summary.getMeeting().getTitle(),
                summary.getRawTranscript()
        );
    }
}
