package com._penLearning.Noddi.domain.qa.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.qa.code.QaApi;
import com._penLearning.Noddi.domain.qa.dto.QaRequestDto;
import com._penLearning.Noddi.domain.qa.dto.QaResponseDto;
import com._penLearning.Noddi.domain.qa.service.QaCommandService;
import com._penLearning.Noddi.domain.qa.service.QaQueryService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class QaController implements QaApi {

    private final QaCommandService qaCommandService;
    private final QaQueryService qaQueryService;

    // 질문 등록
    @Override
    @PostMapping("/api/v1/qa/questions")
    public ApiResponse<QaResponseDto.CreateQuestion> createQuestion(
            @RequestBody @Valid QaRequestDto.CreateQuestion request,
            @AuthenticationPrincipal AuthMember authMember) {

        QaResponseDto.CreateQuestion response = qaCommandService.createQuestion(authMember.getUserId(), request);
        return ApiResponse.onSuccess("질문이 성공적으로 등록되었습니다.", response);
    }

    // 내가 작성한 질문 목록 조회
    @Override
    @GetMapping("/api/v1/qa/questions/me")
    public ApiResponse<Page<QaResponseDto.QuestionInfo>> getMyQuestions(
            @AuthenticationPrincipal AuthMember authMember,
            Pageable pageable) {

        Page<QaResponseDto.QuestionInfo> response = qaQueryService.getMyQuestions(authMember.getUserId(), pageable);
        return ApiResponse.onSuccess("내 질문 목록 조회에 성공했습니다.", response);
    }

    // 특정 팀의 질문 목록 조회 (경로 다름)
    @Override
    @GetMapping("/api/v1/teams/{teamId}/qa/questions")
    public ApiResponse<Page<QaResponseDto.QuestionInfo>> getTeamQuestions(
            @PathVariable Long teamId,
            @AuthenticationPrincipal AuthMember authMember,
            Pageable pageable) {

        Page<QaResponseDto.QuestionInfo> response = qaQueryService.getTeamQuestions(authMember.getUserId(),teamId, pageable);
        return ApiResponse.onSuccess("팀 질문 목록 조회에 성공했습니다.", response);
    }

    // 질문 상세 단건 조회
    @Override
    @GetMapping("/api/v1/qa/questions/{questionId}")
    public ApiResponse<QaResponseDto.QuestionDetail> getQuestionDetail(
            @PathVariable Long questionId,
            @AuthenticationPrincipal AuthMember authMember) {

        QaResponseDto.QuestionDetail response = qaQueryService.getQuestionDetail(authMember.getUserId(), questionId);
        return ApiResponse.onSuccess("질문 상세 조회에 성공했습니다.", response);
    }

    // 직접 답변 등록
    @Override
    @PostMapping("/api/v1/qa/questions/{questionId}/answers")
    public ApiResponse<QaResponseDto.CreateAnswer> createAnswer(
            @PathVariable Long questionId,
            @RequestBody @Valid QaRequestDto.CreateAnswer request,
            @AuthenticationPrincipal AuthMember authMember) {

        QaResponseDto.CreateAnswer response = qaCommandService.createAnswer(questionId, authMember.getUserId(), request);
        return ApiResponse.onSuccess("답변이 성공적으로 등록되었습니다.", response);
    }
}
