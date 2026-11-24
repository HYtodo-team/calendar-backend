package com.hytodo.backend.domain.dashboard.dto;

import java.util.List;

import com.hytodo.backend.domain.event.dto.EventSummaryResponse;

public record DashboardSummaryResponse(
        long todayEventCount,
        long incompleteTodoCount,
        List<EventSummaryResponse> upcomingEvents
) {
}
