package com.hytodo.backend.domain.calendar.dto;

import java.time.LocalDate;

public record CalendarDateResponse(
        LocalDate date
) {
}