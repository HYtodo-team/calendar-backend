package com.hytodo.backend.domain.calendar.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.hytodo.backend.domain.calendar.dto.CalendarDateResponse;

@Service
public class CalendarQueryService {

    public CalendarDateResponse getCalendarDate(Long userId, LocalDate date) {
        return new CalendarDateResponse(date);
    }
}