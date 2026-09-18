package com.hytodo.backend.domain.todo.dto;

import jakarta.validation.constraints.Size;

public record TodoUpdateRequest(
    Long eventId,
    
    @Size(min = 1, max = 255, message = "내용은 1자 이상 255자 이하이어야 합니다.")
    String content
) {}