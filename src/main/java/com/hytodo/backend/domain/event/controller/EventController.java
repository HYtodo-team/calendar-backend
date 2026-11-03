package com.hytodo.backend.domain.event.controller;

import java.time.LocalDate;
import java.util.List;

import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.service.EventService;
import com.hytodo.backend.global.response.ApiResponse;
import com.hytodo.backend.global.security.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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
}