package com._penLearning.Noddi.domain.user.controller;

import com._penLearning.Noddi.domain.auth.entity.AuthMember;
import com._penLearning.Noddi.domain.user.code.UserApi;
import com._penLearning.Noddi.domain.user.dto.UserRequestDto;
import com._penLearning.Noddi.domain.user.dto.UserResponseDto;
import com._penLearning.Noddi.domain.user.service.UserService;
import com._penLearning.Noddi.domain.user.service.UserProfileImageService;
import com._penLearning.Noddi.domain.user.storage.ProfileImageResource;
import com._penLearning.Noddi.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final UserProfileImageService userProfileImageService;

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

        userService.updateProfile(authMember.getUserId(), request);
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

    @Override
    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponseDto.ProfileImageInfo> updateProfileImage(
            @RequestPart("image") org.springframework.web.multipart.MultipartFile image,
            @AuthenticationPrincipal AuthMember authMember
    ) {
        return ApiResponse.onSuccess(
                "프로필 이미지가 성공적으로 등록되었습니다.",
                userProfileImageService.updateProfileImage(authMember.getUserId(), image)
        );
    }

    @Override
    @DeleteMapping("/me/profile-image")
    public ApiResponse<Void> deleteProfileImage(
            @AuthenticationPrincipal AuthMember authMember
    ) {
        userProfileImageService.deleteProfileImage(authMember.getUserId());
        return ApiResponse.onSuccess("프로필 이미지가 삭제되었습니다.", null);
    }

    @Override
    @GetMapping("/{userId}/profile-image")
    public ResponseEntity<Resource> getProfileImage(@PathVariable Long userId) {
        ProfileImageResource image = userProfileImageService.getProfileImage(userId);
        return ResponseEntity.ok()
                .contentType(image.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.resource());
    }
}
