
package com.hytodo.backend.domain.todo.dto;

import com.hytodo.backend.domain.event.entity.Event;
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

        // Todo에 연결된 Event 조회
        Event event = todo.getEvent();

        // 연결된 Event가 있으면 일정 정보 반환
        // 독립 Todo라면 null 반환
        TodoEventSummaryResponse eventResponse = null;

        if (event != null) {
            eventResponse = new TodoEventSummaryResponse(
                    event.getId(),
                    event.getTitle()
            );
        }

        return new TodoResponse(
                todo.getId(),
                todo.getContent(),
                todo.isCompleted(),
                eventResponse,
                todo.getCreatedAt(),
                todo.getUpdatedAt()
        );
    }
}
