package com.hytodo.backend.domain.timetable.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hytodo.backend.domain.timetable.entity.TimetableEntry;

public record TimetableEntryResponse(
		Long id,
		Integer dayOfWeek,
		@JsonFormat(pattern = "HH:mm:ss")
		LocalTime startTime,
		@JsonFormat(pattern = "HH:mm:ss")
		LocalTime endTime,
		String title,
		String location,
		String memo,
		String color
) {

	public static TimetableEntryResponse from(TimetableEntry entry) {
		return new TimetableEntryResponse(
				entry.getId(),
				entry.getDayOfWeek(),
				entry.getStartTime(),
				entry.getEndTime(),
				entry.getTitle(),
				entry.getLocation(),
				entry.getMemo(),
				entry.getColor()
		);
	}
}
