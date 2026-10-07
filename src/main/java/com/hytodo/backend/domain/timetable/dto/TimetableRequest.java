package com.hytodo.backend.domain.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TimetableRequest(

		@NotBlank(message = "제목은 필수입니다.")
		@Size(max = 100, message = "제목은 100자 이하여야 합니다.")
		String title,

		@NotBlank(message = "학기는 필수입니다.")
		@Size(max = 50, message = "학기는 50자 이하여야 합니다.")
		String semester
) {
}
