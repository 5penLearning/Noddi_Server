package com._penLearning.Noddi.domain.project.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.project.dto.ProjectRequestDto;
import com._penLearning.Noddi.domain.project.dto.ProjectResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Project API", description = "조직 내 프로젝트 생성 및 조회 관련 API")
public interface ProjectApi {

    @Operation(summary = "프로젝트 생성", description = "인증된 사용자가 자신이 속한 조직 내에 새로운 프로젝트를 생성합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "프로젝트 생성 성공",
                    content = @Content(schema = @Schema(implementation = ProjectResponseDto.Info.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (이름 누락 등)",
                    content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "유저 정보 조회 실패",
                    content = @Content)
    })
    ApiResponse<ProjectResponseDto.Info> createProject(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember,
            ProjectRequestDto.Create request
    );

    @Operation(summary = "소속 조직 프로젝트 목록 조회", description = "요청한 유저가 속해있는 조직(Organization)의 모든 프로젝트 목록을 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ProjectResponseDto.Info.class)))
    })
    ApiResponse<List<ProjectResponseDto.Info>> getProjects(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "프로젝트 정보 수정", description = "프로젝트 리더가 프로젝트의 이름과 설명을 수정합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (이름 누락 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리더 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    ApiResponse<Void> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody ProjectRequestDto.Update request,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );

    @Operation(summary = "프로젝트 삭제", description = "프로젝트 리더가 프로젝트를 삭제합니다. 관련된 멤버 정보도 함께 삭제됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로젝트 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "리더 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    ApiResponse<Void> deleteProject(
            @PathVariable Long projectId,
            @Parameter(hidden = true) @AuthenticationPrincipal AuthMember authMember
    );
}
