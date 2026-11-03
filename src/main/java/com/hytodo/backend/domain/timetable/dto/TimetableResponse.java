package com.hytodo.backend.domain.timetable.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hytodo.backend.domain.timetable.entity.Timetable;

public record TimetableResponse(
		Long id,
		String title,
		String semester,
		@JsonProperty("isActive")
		boolean isActive,
		LocalDateTime createdAt,
		LocalDateTime updatedAt
) {

	public static TimetableResponse from(Timetable timetable) {
		return new TimetableResponse(
				timetable.getId(),
				timetable.getTitle(),
				timetable.getSemester(),
				timetable.isActive(),
				timetable.getCreatedAt(),
				timetable.getUpdatedAt()
		);
	}
}
