package com.hytodo.backend.domain.event.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.todo.dto.TodoResponse;

public record EventDetailResponse(
        Long id,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        LocalTime startTime,
        LocalTime endTime,
        boolean isAllDay,
        String location,
        String memo,
        boolean isImportant,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TodoResponse> todos
) {

    public static EventDetailResponse from(Event event, List<TodoResponse> todos) {
        return new EventDetailResponse(
                event.getId(),
                event.getTitle(),
                event.getStartDate(),
                event.getEndDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.isAllDay(),
                event.getLocation(),
                event.getMemo(),
                event.isImportant(),
                event.getCreatedAt(),
                event.getUpdatedAt(),
                todos
        );
    }
}
