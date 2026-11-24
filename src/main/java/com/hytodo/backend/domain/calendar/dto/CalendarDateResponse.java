package com.hytodo.backend.domain.calendar.dto;

import java.time.LocalDate;
import java.util.List;

import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;

public record CalendarDateResponse(
        LocalDate date,
        List<EventSummaryResponse> events,
        DailyNoteResponse dailyNote
) {
}