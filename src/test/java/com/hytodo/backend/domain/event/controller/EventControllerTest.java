package com.hytodo.backend.domain.event.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.hytodo.backend.domain.event.dto.EventDetailResponse;
import com.hytodo.backend.domain.event.service.EventService;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import com.hytodo.backend.global.security.CustomUserDetails;
import org.springframework.http.MediaType;

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

    @Test
    void createEventReturnsCreated() throws Exception {
        EventDetailResponse response = detailResponse(10L, true);
        given(eventService.createEvent(org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.any()))
                .willReturn(response);

        mockMvc.perform(post("/api/v1/calendar-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "팀 회의",
                                  "startDate": "2026-09-07",
                                  "endDate": "2026-09-07",
                                  "isAllDay": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void createEventRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/calendar-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "startDate": "2026-09-07",
                                  "endDate": "2026-09-07",
                                  "isAllDay": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        then(eventService).should(org.mockito.Mockito.never()).createEvent(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void getEventReturnsDetail() throws Exception {
        EventDetailResponse response = detailResponse(10L, true);
        given(eventService.getEvent(USER_ID, 10L)).willReturn(response);

        mockMvc.perform(get("/api/v1/calendar-events/{eventId}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.todos").isArray());
    }

    @Test
    void getEventReturnsNotFoundForOtherUsersEvent() throws Exception {
        given(eventService.getEvent(USER_ID, 10L))
                .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "일정을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/calendar-events/{eventId}", 10L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void updateEventAcceptsPartialBody() throws Exception {
        EventDetailResponse response = detailResponse(10L, true);
        given(eventService.updateEvent(org.mockito.ArgumentMatchers.eq(USER_ID), org.mockito.ArgumentMatchers.eq(10L), org.mockito.ArgumentMatchers.any()))
                .willReturn(response);

        mockMvc.perform(patch("/api/v1/calendar-events/{eventId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "변경된 제목"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void updateEventRejectsEmptyBody() throws Exception {
        mockMvc.perform(patch("/api/v1/calendar-events/{eventId}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteEventReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/calendar-events/{eventId}", 10L))
                .andExpect(status().isNoContent());

        then(eventService).should().deleteEvent(USER_ID, 10L);
    }

    private EventDetailResponse detailResponse(Long id, boolean allDay) {
        return new EventDetailResponse(
                id, "팀 회의",
                LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 7),
                null, null, allDay, null, null, false,
                LocalDateTime.now(), LocalDateTime.now(), List.of());
    }
}