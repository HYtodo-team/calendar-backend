package com.hytodo.backend.domain.event.controller;

import java.time.LocalDate;
import java.util.List;

import com.hytodo.backend.domain.event.dto.EventCreateRequest;
import com.hytodo.backend.domain.event.dto.EventDetailResponse;
import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.dto.EventUpdateRequest;
import com.hytodo.backend.domain.event.service.EventService;
import com.hytodo.backend.global.response.ApiResponse;
import com.hytodo.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/calendar-events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EventSummaryResponse>>> getEvents(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        List<EventSummaryResponse> response =
                eventService.getEvents(userDetails.getUserId(), from, to);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-EVT-001: 일정 생성
    @PostMapping
    public ResponseEntity<ApiResponse<EventDetailResponse>> createEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody EventCreateRequest request
    ) {
        EventDetailResponse response = eventService.createEvent(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // API-EVT-003: 일정 상세 및 연결 투두 조회
    @GetMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId
    ) {
        EventDetailResponse response = eventService.getEvent(userDetails.getUserId(), eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-EVT-004: 일정 수정
    @PatchMapping("/{eventId}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> updateEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId,
            @Valid @RequestBody EventUpdateRequest request
    ) {
        EventDetailResponse response = eventService.updateEvent(userDetails.getUserId(), eventId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-EVT-005: 일정 삭제
    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long eventId
    ) {
        eventService.deleteEvent(userDetails.getUserId(), eventId);
        return ResponseEntity.noContent().build();
    }
}