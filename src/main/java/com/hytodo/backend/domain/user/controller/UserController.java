package com.hytodo.backend.domain.user.controller;

import com.hytodo.backend.domain.user.dto.UserResponse;
import com.hytodo.backend.domain.user.service.UserService;
import com.hytodo.backend.global.response.ApiResponse;
import com.hytodo.backend.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // API-USER-001: 내 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        UserResponse response = userService.getMyProfile(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
