package com.hytodo.backend.domain.todo.dto;

import com.hytodo.backend.domain.todo.entity.Todo;
import java.time.LocalDateTime;

public record TodoResponse(
    Long id,
    String content,
    boolean isCompleted,
    TodoEventSummaryResponse event,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TodoResponse from(Todo todo) {
        return new TodoResponse(
            todo.getId(),
            todo.getContent(),
            todo.isCompleted(),
            null, // Event 엔티티 연결 전이므로 null
            todo.getCreatedAt(),
            todo.getUpdatedAt()
        );
    }
}