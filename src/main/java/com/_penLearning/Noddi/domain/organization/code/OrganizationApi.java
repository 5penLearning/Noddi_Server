package com._penLearning.Noddi.domain.organization.code;

import com._penLearning.Noddi.domain.organization.dto.OrganizationResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Organization API", description = "조직(Organization) 정보 조회 API")
public interface OrganizationApi {

    @Operation(summary = "단일 조직 정보 조회", description = "조직 ID를 기반으로 해당 조직의 상세 정보를 조회합니다.")
    ApiResponse<OrganizationResponseDto.Info> getOrganization(
            @Parameter(description = "조직 ID") @PathVariable Long organizationId
    );

    @Operation(summary = "전체 조직 목록 조회", description = "회원가입 시 선택할 수 있는 전체 조직(Organization)의 목록을 조회합니다.")
    ApiResponse<List<OrganizationResponseDto.Info>> getAllOrganizations();
}
