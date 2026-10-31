package com.hytodo.backend.domain.timetable.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 시간표 항목 부분 수정 요청입니다. 전달되지 않은 필드는 변경하지 않으며,
 * {@code location}, {@code memo}, {@code color}는 빈 문자열을 보내면 값을 비웁니다.
 */
public record TimetableEntryUpdateRequest(

		@Min(value = 1, message = "요일은 1(월)~7(일) 사이여야 합니다.")
		@Max(value = 7, message = "요일은 1(월)~7(일) 사이여야 합니다.")
		Integer dayOfWeek,

		LocalTime startTime,

		LocalTime endTime,

		@Size(max = 100, message = "제목은 100자 이하여야 합니다.")
		String title,

		@Size(max = 255, message = "장소는 255자 이하여야 합니다.")
		String location,

		String memo,

		@Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "색상은 #RRGGBB 형식이거나 빈 문자열이어야 합니다.")
		String color
) {

	@AssertTrue(message = "수정할 값을 하나 이상 포함해야 합니다.")
	public boolean isAnyFieldPresent() {
		return dayOfWeek != null
				|| startTime != null
				|| endTime != null
				|| title != null
				|| location != null
				|| memo != null
				|| color != null;
	}

	@AssertTrue(message = "제목은 공백일 수 없습니다.")
	public boolean isTitlePresentThenNotBlank() {
		return title == null || !title.isBlank();
	}

	@AssertTrue(message = "시작 시간과 종료 시간은 함께 전달해야 합니다.")
	public boolean isTimeRangePairedOrAbsent() {
		return (startTime == null) == (endTime == null);
	}
}
