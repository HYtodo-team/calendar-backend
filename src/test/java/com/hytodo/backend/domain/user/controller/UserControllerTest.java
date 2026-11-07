package com.hytodo.backend.domain.user.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.hytodo.backend.domain.user.dto.UserResponse;
import com.hytodo.backend.domain.user.service.UserService;
import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import com.hytodo.backend.global.security.CustomUserDetails;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserControllerTest {

    private static final Long USER_ID = 1L;

    private MockMvc mockMvc;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = Mockito.mock(UserService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(USER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyProfileReturnsAuthenticatedUsersProfile() throws Exception {
        UserResponse response = new UserResponse(
                USER_ID, "user@example.com", "tester", LocalDateTime.now());
        given(userService.getMyProfile(USER_ID)).willReturn(response);

        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("tester"))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }
}
