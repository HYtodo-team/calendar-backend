package com.hytodo.backend.domain.timetable.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hytodo.backend.domain.timetable.entity.Timetable;

/**
 * 목록 응답에 {@code entries}가 추가된 상세 응답입니다.
 * {@code entries}는 요일과 시작 시간 오름차순으로 정렬된 시간표 항목입니다.
 */
public record TimetableDetailResponse(
		Long id,
		String title,
		String semester,
		@JsonProperty("isActive")
		boolean isActive,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		List<TimetableEntryResponse> entries
) {

	public static TimetableDetailResponse from(Timetable timetable, List<TimetableEntryResponse> entries) {
		return new TimetableDetailResponse(
				timetable.getId(),
				timetable.getTitle(),
				timetable.getSemester(),
				timetable.isActive(),
				timetable.getCreatedAt(),
				timetable.getUpdatedAt(),
				entries
		);
	}
}
