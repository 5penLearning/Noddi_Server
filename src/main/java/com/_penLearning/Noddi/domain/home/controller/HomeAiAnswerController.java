package com._penLearning.Noddi.domain.home.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.home.code.HomeAiAnswerApi;
import com._penLearning.Noddi.domain.home.dto.HomeAiAnswerResponseDto;
import com._penLearning.Noddi.domain.home.service.HomeAiAnswerQueryService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/home/ai-answers")
@RequiredArgsConstructor
public class HomeAiAnswerController implements HomeAiAnswerApi {

    private final HomeAiAnswerQueryService homeAiAnswerQueryService;

    @Override
    @GetMapping("/projects")
    public ApiResponse<List<HomeAiAnswerResponseDto.ProjectStatus>>
    getUnreadAnswerCountsByProject(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        List<HomeAiAnswerResponseDto.ProjectStatus> response =
                homeAiAnswerQueryService.getUnreadAnswerCountsByProject(
                        authMember.getUserId()
                );

        return ApiResponse.onSuccess(
                "프로젝트별 미확인 AI 답변 현황 조회에 성공했습니다.",
                response
        );
    }

    @Override
    @GetMapping("/projects/{projectId}")
    public ApiResponse<HomeAiAnswerResponseDto.CardPage>
    getUnreadAnswerCards(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        HomeAiAnswerResponseDto.CardPage response =
                homeAiAnswerQueryService.getUnreadAnswerCards(
                        authMember.getUserId(),
                        projectId,
                        page,
                        size
                );

        return ApiResponse.onSuccess(
                "미확인 AI 답변 카드 조회에 성공했습니다.",
                response
        );
    }
}
