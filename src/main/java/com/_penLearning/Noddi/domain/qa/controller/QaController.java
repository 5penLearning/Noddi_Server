package com._penLearning.Noddi.domain.qa.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.qa.code.QaApi;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.service.QaQuestionCommandService;
import com._penLearning.Noddi.domain.qa.service.QaAnswerUpdateService;
import com._penLearning.Noddi.domain.qa.service.QaQuestionQueryService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class QaController implements QaApi {

    private final QaQuestionCommandService qaQuestionCommandService;
    private final QaQuestionQueryService qaQuestionQueryService;
    private final QaAnswerUpdateService qaAnswerUpdateService;

    // 질문 등록
    @Override
    @PostMapping("/api/v1/qa/questions")
    public ApiResponse<QaResponseDto.CreateQuestion> createQuestion(
            @RequestBody @Valid QaRequestDto.CreateQuestion request,
            @AuthenticationPrincipal AuthMember authMember) {

        QaResponseDto.CreateQuestion response = qaQuestionCommandService.createQuestion(authMember.getUserId(), request);
        return ApiResponse.onSuccess("질문이 성공적으로 등록되었습니다.", response);
    }

    // 내가 작성한 질문 목록 조회
    @Override
    @GetMapping("/api/v1/qa/questions/me")
    public ApiResponse<Page<QaResponseDto.QuestionInfo>> getMyQuestions(
            @AuthenticationPrincipal AuthMember authMember,
            Pageable pageable) {

        Page<QaResponseDto.QuestionInfo> response = qaQuestionQueryService.getMyQuestions(authMember.getUserId(), pageable);
        return ApiResponse.onSuccess("내 질문 목록 조회에 성공했습니다.", response);
    }

    // 특정 팀의 질문 목록 조회 (경로 다름)
    @Override
    @GetMapping("/api/v1/teams/{teamId}/qa/questions")
    public ApiResponse<Page<QaResponseDto.QuestionInfo>> getTeamQuestions(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            Pageable pageable) {

        Page<QaResponseDto.QuestionInfo> response = qaQuestionQueryService.getTeamQuestions(authMember.getUserId(),teamId, pageable);
        return ApiResponse.onSuccess("팀 질문 목록 조회에 성공했습니다.", response);
    }

    // 질문 상세 단건 조회
    @Override
    @GetMapping("/api/v1/qa/questions/{questionId}")
    public ApiResponse<QaResponseDto.QuestionDetail> getQuestionDetail(
            @PathVariable Long questionId,
            @AuthenticationPrincipal AuthMember authMember) {

        QaResponseDto.QuestionDetail response = qaQuestionQueryService.getQuestionDetail(authMember.getUserId(), questionId);
        return ApiResponse.onSuccess("질문 상세 조회에 성공했습니다.", response);
    }

    // AI 답변 수정
    @Override
    @PatchMapping("/api/v1/qa/answers/{answerId}")
    public ApiResponse<QaResponseDto.ReviseAnswer> reviseAnswer(
            @PathVariable Long answerId,
            @RequestBody @Valid QaRequestDto.ReviseAnswer request,
            @AuthenticationPrincipal AuthMember authMember) {
        QaResponseDto.ReviseAnswer response = qaAnswerUpdateService.revise(
                answerId,
                authMember.getUserId(),
                request
        );
        return ApiResponse.onSuccess("AI 답변이 성공적으로 수정되었습니다.", response);
    }

}
