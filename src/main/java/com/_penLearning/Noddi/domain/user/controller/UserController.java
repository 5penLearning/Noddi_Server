package com._penLearning.Noddi.domain.user.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.user.code.UserApi;
import com._penLearning.Noddi.domain.user.dto.UserRequestDto;
import com._penLearning.Noddi.domain.user.dto.UserResponseDto;
import com._penLearning.Noddi.domain.user.service.UserService;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    // 내 프로필 조회
    @Override
    @GetMapping("/me")
    public ApiResponse<UserResponseDto.ProfileInfo> getMyProfile(
            @AuthenticationPrincipal AuthMember authMember) {
        UserResponseDto.ProfileInfo response = userService.getMyProfile(authMember.getUserId());
        return ApiResponse.onSuccess("프로필 조회에 성공했습니다.", response);
    }

    // 내 프로필 수정
    @Override
    @PatchMapping("/me")
    public ApiResponse<Void> updateProfile(
            @RequestBody @Valid UserRequestDto.UpdateProfile request,
            @AuthenticationPrincipal AuthMember authMember) {

        userService.updateProfile(authMember.getUserId(), request.getName());
        return ApiResponse.onSuccess("프로필이 성공적으로 수정되었습니다.", null);
    }

    // 비밀번호 변경
    @Override
    @PatchMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @RequestBody @Valid UserRequestDto.UpdatePassword request,
            @AuthenticationPrincipal AuthMember authMember) {

        userService.updatePassword(authMember.getUserId(), request);
        return ApiResponse.onSuccess("비밀번호가 성공적으로 변경되었습니다.", null);
    }
}