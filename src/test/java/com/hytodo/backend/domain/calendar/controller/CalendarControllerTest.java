
package com.hytodo.backend.domain.calendar.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hytodo.backend.domain.calendar.dto.CalendarDateResponse;
import com.hytodo.backend.domain.calendar.service.CalendarQueryService;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import com.hytodo.backend.global.security.CustomUserDetails;

class CalendarControllerTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE =
            LocalDate.of(2026, 11, 24);

    private MockMvc mockMvc;
    private CalendarQueryService calendarQueryService;

    @BeforeEach
    void setUp() {
        calendarQueryService = Mockito.mock(CalendarQueryService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new CalendarController(calendarQueryService))
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
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
    void getCalendarDateReturnsOkWithEventsAndDailyNote() throws Exception {

        EventSummaryResponse event = new EventSummaryResponse(
                10L,
                "팀 회의",
                DATE,
                DATE,
                null,
                null,
                true,
                "회의실",
                false
        );

        DailyNoteResponse dailyNote = new DailyNoteResponse(
                20L,
                DATE,
                "오늘의 메모",
                LocalDateTime.of(2026, 11, 24, 12, 0)
        );

        CalendarDateResponse response = new CalendarDateResponse(
                DATE,
                List.of(event),
                dailyNote
        );

        given(calendarQueryService.getCalendarDate(USER_ID, DATE))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/calendar/dates/{date}", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value("2026-11-24"))
                .andExpect(jsonPath("$.data.events").isArray())
                .andExpect(jsonPath("$.data.events[0].id").value(10))
                .andExpect(jsonPath("$.data.events[0].title").value("팀 회의"))
                .andExpect(jsonPath("$.data.dailyNote.id").value(20))
                .andExpect(jsonPath("$.data.dailyNote.content")
                        .value("오늘의 메모"));

        then(calendarQueryService)
                .should()
                .getCalendarDate(USER_ID, DATE);
    }

    @Test
    void getCalendarDateReturnsEmptyDataWhenNothingExists() throws Exception {

        CalendarDateResponse response = new CalendarDateResponse(
                DATE,
                List.of(),
                DailyNoteResponse.empty(DATE)
        );

        given(calendarQueryService.getCalendarDate(USER_ID, DATE))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/calendar/dates/{date}", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.events").isEmpty())
                .andExpect(jsonPath("$.data.dailyNote.noteDate")
                        .value("2026-11-24"))
                .andExpect(jsonPath("$.data.dailyNote.content")
                        .doesNotExist());
    }

    @Test
    void invalidDateReturnsBadRequest() throws Exception {

        mockMvc.perform(get(
                        "/api/v1/calendar/dates/{date}",
                        "invalid-date"
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode")
                        .value("VALIDATION_ERROR"));
    }

    @Test
    void responseDoesNotIncludeTodosOrTimetableEntries() throws Exception {

        CalendarDateResponse response = new CalendarDateResponse(
                DATE,
                List.of(),
                DailyNoteResponse.empty(DATE)
        );

        given(calendarQueryService.getCalendarDate(USER_ID, DATE))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/calendar/dates/{date}", DATE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todos").doesNotExist())
                .andExpect(jsonPath("$.data.timetableEntries").doesNotExist());
    }
}
