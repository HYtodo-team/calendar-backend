package com.hytodo.backend.domain.event.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.hytodo.backend.domain.event.entity.Event;

public record EventSummaryResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean isAllDay,
        String location,
        boolean isImportant
) {

    public static EventSummaryResponse from(Event event) {
        return new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.isAllDay(),
                event.getLocation(),
                event.isImportant()
        );
    }
}