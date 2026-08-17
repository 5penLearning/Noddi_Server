package com._penLearning.Noddi.domain.user.code;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.user.dto.UserRequestDto;
import com._penLearning.Noddi.domain.user.dto.UserResponseDto;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User API", description = "유저 프로필 및 개인정보 관리 API")
public interface UserApi {

    @Operation(summary = "내 프로필 및 조직 정보 조회", description = "현재 로그인한 유저의 개인 프로필과 소속된 조직(Organization) 정보를 조회합니다.")
    ApiResponse<UserResponseDto.ProfileInfo> getMyProfile(
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "내 프로필 수정", description = "현재 로그인한 유저의 이름, 부서, 직함을 수정합니다.")
    ApiResponse<Void> updateProfile(
            UserRequestDto.UpdateProfile request,
            @Parameter(hidden = true) AuthMember authMember
    );

    @Operation(summary = "비밀번호 변경", description = "기존 비밀번호를 확인한 후 새로운 비밀번호로 변경합니다.")
    ApiResponse<Void> updatePassword(
            UserRequestDto.UpdatePassword request,
            @Parameter(hidden = true) AuthMember authMember
    );
}
