
package com.hytodo.backend.domain.dashboard.controller;

import com.hytodo.backend.domain.dashboard.dto.DashboardSummaryResponse;
import com.hytodo.backend.domain.dashboard.service.DashboardService;
import com.hytodo.backend.global.response.ApiResponse;
import com.hytodo.backend.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // API-DASH-001: 대시보드 요약 조회
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        DashboardSummaryResponse response =
                dashboardService.getSummary(userDetails.getUserId());

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
