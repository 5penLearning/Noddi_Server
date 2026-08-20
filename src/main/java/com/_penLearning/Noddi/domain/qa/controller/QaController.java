package com._penLearning.Noddi.domain.qa.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.qa.code.QaApi;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.service.*;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class QaController implements QaApi {

    private final QaQuestionCommandService qaQuestionCommandService;
    private final QaQuestionQueryService qaQuestionQueryService;
    private final QaAnswerUpdateService qaAnswerUpdateService;
    private final QaAnswerRevisionQueryService qaAnswerRevisionQueryService;
    private final QaAnswerStreamSubscriptionService qaAnswerStreamSubscriptionService;


    // 질문 등록
    @Override
    @PostMapping("/qa/questions")
    public ApiResponse<QaResponseDto.CreateQuestion> createQuestion(
            @RequestBody @Valid QaRequestDto.CreateQuestion request,
            @AuthenticationPrincipal AuthMember authMember) {

        QaResponseDto.CreateQuestion response = qaQuestionCommandService.createQuestion(authMember.getUserId(), request);
        return ApiResponse.onSuccess("질문이 성공적으로 등록되었습니다.", response);
    }

    // 내가 작성한 질문 목록 조회
    @Override
    @GetMapping("/qa/questions/me")
    public ApiResponse<Page<QaResponseDto.QuestionInfo>> getMyQuestions(
            @AuthenticationPrincipal AuthMember authMember,
            Pageable pageable) {

        Page<QaResponseDto.QuestionInfo> response = qaQuestionQueryService.getMyQuestions(authMember.getUserId(), pageable);
        return ApiResponse.onSuccess("내 질문 목록 조회에 성공했습니다.", response);
    }

    // 특정 팀의 질문 목록 조회 (경로 다름)
    @Override
    @GetMapping("/teams/{teamId}/qa/questions")
    public ApiResponse<Page<QaResponseDto.QuestionInfo>> getTeamQuestions(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            Pageable pageable) {

        Page<QaResponseDto.QuestionInfo> response = qaQuestionQueryService.getTeamQuestions(authMember.getUserId(),teamId, pageable);
        return ApiResponse.onSuccess("팀 질문 목록 조회에 성공했습니다.", response);
    }

    // 특정 팀의 질문과 답변 피드 조회
    @Override
    @GetMapping("/teams/{teamId}/qa/feed")
    public ApiResponse<QaResponseDto.Feed> getTeamFeed(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        QaResponseDto.Feed response = qaQuestionQueryService.getTeamFeed(authMember.getUserId(), teamId, cursor, size);

        return ApiResponse.onSuccess("팀 Q&A 피드 조회에 성공했습니다.", response);
    }

    // 질문 상세 단건 조회
    @Override
    @GetMapping("/qa/questions/{questionId}")
    public ApiResponse<QaResponseDto.QuestionDetail> getQuestionDetail(
            @PathVariable Long questionId,
            @AuthenticationPrincipal AuthMember authMember) {

        QaResponseDto.QuestionDetail response = qaQuestionQueryService.getQuestionDetail(authMember.getUserId(), questionId);
        return ApiResponse.onSuccess("질문 상세 조회에 성공했습니다.", response);
    }

    // AI 답변 수정 또는 최종 실패 질문에 대한 대상 팀의 직접 답변
    @Override
    @PatchMapping("/qa/answers/{answerId}")
    public ApiResponse<QaResponseDto.ReviseAnswer> reviseAnswer(
            @PathVariable Long answerId,
            @RequestBody @Valid QaRequestDto.ReviseAnswer request,
            @AuthenticationPrincipal AuthMember authMember) {
        QaResponseDto.ReviseAnswer response = qaAnswerUpdateService.revise(
                answerId,
                authMember.getUserId(),
                request
        );
        return ApiResponse.onSuccess("답변이 성공적으로 저장되었습니다.", response);
    }

    /**
     * 특정 답변의 AI 최초 원문과 담당자 수정본을
     * versionNumber 오름차순으로 조회한다.
     */
    @Override
    @GetMapping("/qa/answers/{answerId}/revisions")
    public ApiResponse<QaResponseDto.AnswerRevisionHistory> getAnswerRevisions(
            @PathVariable Long answerId,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        QaResponseDto.AnswerRevisionHistory response = qaAnswerRevisionQueryService.getAnswerRevisions(
                        authMember.getUserId(),
                        answerId
                );

        return ApiResponse.onSuccess("답변 수정 이력 조회에 성공했습니다.", response);
    }

    /**
     * 특정 질문의 AI 답변 생성 과정을 SSE로 구독한다.
     *
     * 일반 API처럼 한 번 응답하고 종료하는 것이 아니라,
     * AI 답변이 완료되거나 실패할 때까지 HTTP 연결을 유지한다.
     */
    @Override
    @GetMapping(value = "/qa/questions/{questionId}/answer-stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeAnswerStream(
            @PathVariable Long questionId,
            @AuthenticationPrincipal AuthMember authMember
    )
    {
        return qaAnswerStreamSubscriptionService.subscribe(authMember.getUserId(), questionId);
    }
}
