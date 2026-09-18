package com.hytodo.backend.domain.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TodoCreateRequest(
    Long eventId,
    
    @NotBlank(message = "내용은 필수입니다.")
    @Size(min = 1, max = 255, message = "내용은 1자 이상 255자 이하이어야 합니다.")
    String content
) {}