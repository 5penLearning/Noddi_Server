package com._penLearning.Noddi.domain.organization.controller;

import com._penLearning.Noddi.domain.organization.code.OrganizationApi;
import com._penLearning.Noddi.domain.organization.dto.OrganizationResponseDto;
import com._penLearning.Noddi.domain.organization.service.OrganizationService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController implements OrganizationApi {

    private final OrganizationService organizationService;

    @Override
    @GetMapping("/{organizationId}")
    public ApiResponse<OrganizationResponseDto.Info> getOrganization(@PathVariable Long organizationId) {

        OrganizationResponseDto.Info response = organizationService.getOrganization(organizationId);
        return ApiResponse.onSuccess("조직 정보 조회에 성공했습니다.", response);
    }

    @Override
    @GetMapping
    public ApiResponse<List<OrganizationResponseDto.Info>> getAllOrganizations() {

        List<OrganizationResponseDto.Info> response = organizationService.getAllOrganizations();
        return ApiResponse.onSuccess("전체 조직 목록 조회에 성공했습니다.", response);
    }
}
