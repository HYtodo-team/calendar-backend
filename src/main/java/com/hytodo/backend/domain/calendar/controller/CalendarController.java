package com.hytodo.backend.domain.calendar.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hytodo.backend.domain.calendar.dto.CalendarDateResponse;
import com.hytodo.backend.domain.calendar.service.CalendarQueryService;
import com.hytodo.backend.global.security.CustomUserDetails;

@RestController
@RequestMapping("/api/v1/calendar")
public class CalendarController {

    private final CalendarQueryService calendarQueryService;

    public CalendarController(CalendarQueryService calendarQueryService) {
        this.calendarQueryService = calendarQueryService;
    }

    @GetMapping("/dates/{date}")
    public CalendarDateResponse getCalendarDate(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return calendarQueryService.getCalendarDate(
                userDetails.getUserId(),
                date
        );
    }
}