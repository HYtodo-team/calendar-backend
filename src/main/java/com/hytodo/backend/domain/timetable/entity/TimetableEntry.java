package com.hytodo.backend.domain.timetable.entity;

import java.time.LocalTime;
import java.util.regex.Pattern;

import com.hytodo.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "timetable_entries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TimetableEntry extends BaseTimeEntity {

	private static final int MAX_TITLE_LENGTH = 100;
	private static final int MAX_LOCATION_LENGTH = 255;
	private static final int COLOR_LENGTH = 7;
	private static final int MIN_DAY_OF_WEEK = 1;
	private static final int MAX_DAY_OF_WEEK = 7;
	private static final Pattern COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "timetable_id", nullable = false)
	private Timetable timetable;

	@Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
	private String title;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "day_of_week", nullable = false)
	private Integer dayOfWeek;

	@Column(name = "start_time", nullable = false)
	private LocalTime startTime;

	@Column(name = "end_time", nullable = false)
	private LocalTime endTime;

	@Column(name = "location", length = MAX_LOCATION_LENGTH)
	private String location;

	@Column(name = "memo")
	private String memo;

	@Column(name = "color", length = COLOR_LENGTH)
	private String color;

	@Builder
	private TimetableEntry(
		Timetable timetable,
		String title,
		Integer dayOfWeek,
		LocalTime startTime,
		LocalTime endTime,
		String location,
		String memo,
		String color
	) {
		validateTimetable(timetable);
		validateTitle(title);
		validateDayOfWeek(dayOfWeek);
		validateTime(startTime, endTime);
		validateLocation(location);
		validateColor(color);
		this.timetable = timetable;
		this.title = title;
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
		this.location = location;
		this.memo = memo;
		this.color = color;
	}

	public void changeTitle(String title) {
		validateTitle(title);
		this.title = title;
	}

	public void changeDayOfWeek(Integer dayOfWeek) {
		validateDayOfWeek(dayOfWeek);
		this.dayOfWeek = dayOfWeek;
	}

	public void changeTime(LocalTime startTime, LocalTime endTime) {
		validateTime(startTime, endTime);
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public void changeLocation(String location) {
		validateLocation(location);
		this.location = location;
	}

	public void changeMemo(String memo) {
		this.memo = memo;
	}

	public void changeColor(String color) {
		validateColor(color);
		this.color = color;
	}

	private static void validateTimetable(Timetable timetable) {
		if (timetable == null) {
			throw new IllegalArgumentException("timetable must not be null");
		}
	}

	private static void validateTitle(String title) {
		validateNotBlank(title, "title");
		validateMaxLength(title, MAX_TITLE_LENGTH, "title");
	}

	private static void validateDayOfWeek(Integer dayOfWeek) {
		if (dayOfWeek == null) {
			throw new IllegalArgumentException("dayOfWeek must not be null");
		}
		if (dayOfWeek < MIN_DAY_OF_WEEK || dayOfWeek > MAX_DAY_OF_WEEK) {
			throw new IllegalArgumentException(
				"dayOfWeek must be between " + MIN_DAY_OF_WEEK + " and " + MAX_DAY_OF_WEEK);
		}
	}

	private static void validateTime(LocalTime startTime, LocalTime endTime) {
		if (startTime == null) {
			throw new IllegalArgumentException("startTime must not be null");
		}
		if (endTime == null) {
			throw new IllegalArgumentException("endTime must not be null");
		}
		if (!startTime.isBefore(endTime)) {
			throw new IllegalArgumentException("startTime must be before endTime");
		}
	}

	private static void validateLocation(String location) {
		if (location != null) {
			validateMaxLength(location, MAX_LOCATION_LENGTH, "location");
		}
	}

	private static void validateColor(String color) {
		if (color != null && !COLOR_PATTERN.matcher(color).matches()) {
			throw new IllegalArgumentException("color must match #RRGGBB format");
		}
	}

	private static void validateNotBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
	}

	private static void validateMaxLength(String value, int maxLength, String fieldName) {
		if (value.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must be less than or equal to " + maxLength + " characters");
		}
	}
}
