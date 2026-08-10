package com._penLearning.Noddi.domain.project.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.project.code.ProjectApi;
import com._penLearning.Noddi.domain.project.dto.ProjectRequestDto;
import com._penLearning.Noddi.domain.project.dto.ProjectResponseDto;
import com._penLearning.Noddi.domain.project.service.ProjectService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController implements ProjectApi {

    private final ProjectService projectService;

    @Override
    @PostMapping
    public ApiResponse<ProjectResponseDto.Info> createProject(
            @AuthenticationPrincipal AuthMember authMember,
            @RequestBody @Valid ProjectRequestDto.Create request) {

        ProjectResponseDto.Info response = projectService.createProject(authMember.getUserId(), request);
        return ApiResponse.onSuccess("프로젝트가 성공적으로 생성되었습니다.", response);
    }

    @Override
    @GetMapping
    public ApiResponse<List<ProjectResponseDto.Info>> getProjects(
            @AuthenticationPrincipal AuthMember authMember) {

        List<ProjectResponseDto.Info> response = projectService.getProjectsByOrganization(authMember.getUserId());
        return ApiResponse.onSuccess("프로젝트 목록 조회에 성공했습니다.", response);
    }

    @Override
    @PatchMapping("/{projectId}")
    public ApiResponse<Void> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequestDto.Update request,
            @AuthenticationPrincipal AuthMember authMember){
            projectService.updateProject(authMember.getUserId(), projectId, request);

            return ApiResponse.onSuccess("프로젝트 정보 수정을 성공했습니다.");
    }

    @Override
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember authMember) {

        projectService.deleteProject(projectId, authMember.getUserId());
        return ApiResponse.onSuccess("프로젝트가 성공적으로 삭제되었습니다.");
    }
}
