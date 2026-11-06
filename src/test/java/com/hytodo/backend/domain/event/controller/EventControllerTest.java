package com.hytodo.backend.domain.event.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.hytodo.backend.domain.event.service.EventService;
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

class EventControllerTest {

    private static final Long USER_ID = 1L;

    private MockMvc mockMvc;
    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = Mockito.mock(EventService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(eventService))
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(USER_ID);

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
    void getEventsReturnsOk() throws Exception {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 30);

        given(eventService.getEvents(USER_ID, from, to))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/calendar-events")
                        .param("from", "2026-11-01")
                        .param("to", "2026-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        then(eventService).should().getEvents(USER_ID, from, to);
    }

    @Test
    void invalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/calendar-events")
                        .param("from", "invalid")
                        .param("to", "2026-11-30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_ERROR"));
    }

    @Test
    void missingDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/calendar-events")
                        .param("from", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }
}