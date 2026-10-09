package com.hytodo.backend.domain.timetable.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * 부분 수정 요청입니다. 전달된 필드만 변경하며, 전달된 값은 공백일 수 없습니다.
 */
public record TimetableUpdateRequest(

		@Size(max = 100, message = "제목은 100자 이하여야 합니다.")
		String title,

		@Size(max = 50, message = "학기는 50자 이하여야 합니다.")
		String semester
) {

	@AssertTrue(message = "수정할 값을 하나 이상 포함해야 합니다.")
	public boolean isAnyFieldPresent() {
		return title != null || semester != null;
	}

	@AssertTrue(message = "제목은 공백일 수 없습니다.")
	public boolean isTitlePresentThenNotBlank() {
		return title == null || !title.isBlank();
	}

	@AssertTrue(message = "학기는 공백일 수 없습니다.")
	public boolean isSemesterPresentThenNotBlank() {
		return semester == null || !semester.isBlank();
	}
}
