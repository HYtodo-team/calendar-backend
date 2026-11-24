
package com.hytodo.backend.domain.dashboard.controller;

import com.hytodo.backend.domain.dashboard.dto.DashboardSummaryResponse;
import com.hytodo.backend.domain.dashboard.service.DashboardService;
import com.hytodo.backend.global.response.ApiResponse;
import com.hytodo.backend.global.security.CustomUserDetails;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DashboardControllerTest {

    private static final Long USER_ID = 1L;

    private MockMvc mockMvc;
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = mock(DashboardService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DashboardController(dashboardService))
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .build();

        CustomUserDetails userDetails =
                new CustomUserDetails(USER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getSummaryReturnsDashboardData() throws Exception {
        DashboardSummaryResponse response =
                new DashboardSummaryResponse(
                        3L,
                        5L,
                        List.of()
                );

        given(dashboardService.getSummary(USER_ID))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.todayEventCount").value(3))
                .andExpect(jsonPath("$.data.incompleteTodoCount").value(5))
                .andExpect(jsonPath("$.data.upcomingEvents").isArray())
                .andExpect(jsonPath("$.data.upcomingEvents").isEmpty());

        verify(dashboardService).getSummary(USER_ID);
    }
}
