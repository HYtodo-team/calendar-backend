package com.hytodo.backend.domain.dailynote.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.dailynote.service.DailyNoteService;
import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import com.hytodo.backend.global.security.CustomUserDetails;

class DailyNoteControllerTest {

    private static final Long USER_ID = 1L;

    private MockMvc mockMvc;
    private DailyNoteService dailyNoteService;

    @BeforeEach
    void setUp() {
        dailyNoteService = Mockito.mock(DailyNoteService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DailyNoteController(dailyNoteService))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
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
    void getNoteReturnsContentWhenExists() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);
        DailyNoteResponse response = new DailyNoteResponse(10L, date, "오늘의 메모", LocalDateTime.now());

        given(dailyNoteService.getNote(USER_ID, date)).willReturn(response);

        mockMvc.perform(get("/api/v1/daily-notes/{date}", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("오늘의 메모"));
    }

    @Test
    void getNoteReturnsNullContentWhenMissing() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);
        DailyNoteResponse response = DailyNoteResponse.empty(date);

        given(dailyNoteService.getNote(USER_ID, date)).willReturn(response);

        mockMvc.perform(get("/api/v1/daily-notes/{date}", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").hasJsonPath())
                .andExpect(jsonPath("$.data.content").doesNotExist());
    }

    @Test
    void getNoteWithInvalidDateReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/daily-notes/{date}", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("date"));
    }

    @Test
    void putNoteReturnsOk() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);
        DailyNoteResponse response = new DailyNoteResponse(10L, date, "저장된 메모", LocalDateTime.now());

        given(dailyNoteService.upsertNote(
                ArgumentMatchers.eq(USER_ID),
                ArgumentMatchers.eq(date),
                ArgumentMatchers.any()))
                .willReturn(response);

        mockMvc.perform(put("/api/v1/daily-notes/{date}", date)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "저장된 메모"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("저장된 메모"));
    }

    @Test
    void putNoteWithNonexistentUserReturnsNotFound() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);

        given(dailyNoteService.upsertNote(
                ArgumentMatchers.eq(USER_ID),
                ArgumentMatchers.eq(date),
                ArgumentMatchers.any()))
                .willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        mockMvc.perform(put("/api/v1/daily-notes/{date}", date)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                              "content": "메모"
                            }
                            """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void putNoteRejectsBlankContent() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);

        mockMvc.perform(put("/api/v1/daily-notes/{date}", date)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("content"));

        then(dailyNoteService).should(Mockito.never())
                .upsertNote(ArgumentMatchers.anyLong(), ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    void deleteNoteReturnsNoContent() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);

        mockMvc.perform(delete("/api/v1/daily-notes/{date}", date))
                .andExpect(status().isNoContent());

        then(dailyNoteService).should().deleteNote(USER_ID, date);
    }

    @Test
    void deleteNoteWhenMissingStillReturnsNoContent() throws Exception {
        LocalDate date = LocalDate.of(2026, 9, 7);

        mockMvc.perform(delete("/api/v1/daily-notes/{date}", date))
                .andExpect(status().isNoContent());
    }
}
