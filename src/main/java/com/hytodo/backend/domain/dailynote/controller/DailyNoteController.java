package com.hytodo.backend.domain.dailynote.controller;

import java.time.LocalDate;

import com.hytodo.backend.domain.dailynote.dto.DailyNoteRequest;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.dailynote.service.DailyNoteService;
import com.hytodo.backend.global.response.ApiResponse;
import com.hytodo.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/daily-notes")
@RequiredArgsConstructor
public class DailyNoteController {

    private final DailyNoteService dailyNoteService;

    // API-NOTE-001: 날짜 메모 조회
    @GetMapping("/{date}")
    public ResponseEntity<ApiResponse<DailyNoteResponse>> getNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        DailyNoteResponse response = dailyNoteService.getNote(userDetails.getUserId(), date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-NOTE-002: 날짜 메모 작성·수정
    @PutMapping("/{date}")
    public ResponseEntity<ApiResponse<DailyNoteResponse>> upsertNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody DailyNoteRequest request
    ) {
        DailyNoteResponse response = dailyNoteService.upsertNote(userDetails.getUserId(), date, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-NOTE-003: 날짜 메모 삭제
    @DeleteMapping("/{date}")
    public ResponseEntity<Void> deleteNote(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        dailyNoteService.deleteNote(userDetails.getUserId(), date);
        return ResponseEntity.noContent().build();
    }
}
