package com.hytodo.backend.domain.timetable.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

/**
 * 활성화 요청입니다. 전체 비활성화를 막기 위해 {@code true}만 허용합니다.
 */
public record TimetableActivationRequest(

		@NotNull(message = "isActive는 필수입니다.")
		@AssertTrue(message = "isActive는 true만 허용합니다.")
		@JsonProperty("isActive")
		Boolean isActive
) {
}
