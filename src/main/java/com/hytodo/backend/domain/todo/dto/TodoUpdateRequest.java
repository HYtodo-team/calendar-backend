package com.hytodo.backend.domain.todo.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TodoUpdateRequest(
    Long eventId,
    
    @Size(max = 255, message = "내용은 255자 이하이어야 합니다.")
    @Pattern(regexp = "(?s).*\\S.*", message = "내용은 공백일 수 없습니다.")
    String content
) {}