package com.hytodo.backend.domain.timetable.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 시간표 항목 생성 요청입니다. 시간 형식은 {@code HH:mm:ss}입니다.
 */
public record TimetableEntryRequest(

		@NotNull(message = "요일은 필수입니다.")
		@Min(value = 1, message = "요일은 1(월)~7(일) 사이여야 합니다.")
		@Max(value = 7, message = "요일은 1(월)~7(일) 사이여야 합니다.")
		Integer dayOfWeek,

		@NotNull(message = "시작 시간은 필수입니다.")
		LocalTime startTime,

		@NotNull(message = "종료 시간은 필수입니다.")
		LocalTime endTime,

		@NotBlank(message = "제목은 필수입니다.")
		@Size(max = 100, message = "제목은 100자 이하여야 합니다.")
		String title,

		@Size(max = 255, message = "장소는 255자 이하여야 합니다.")
		String location,

		String memo,

		@Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상은 #RRGGBB 형식이어야 합니다.")
		String color
) {
}
