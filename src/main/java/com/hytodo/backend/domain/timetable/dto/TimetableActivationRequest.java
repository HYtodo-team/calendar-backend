package com.hytodo.backend.domain.timetable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * 활성화 상태 변경 요청입니다. {@code true}는 활성화, {@code false}는 비활성화입니다.
 */
public record TimetableActivationRequest(

		@NotNull(message = "isActive는 필수입니다.")
		@JsonProperty("isActive")
		Boolean isActive
) {
}
