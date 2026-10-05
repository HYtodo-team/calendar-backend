package com.hytodo.backend.domain.timetable.entity;

import com.hytodo.backend.domain.user.entity.User;
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

@Getter
@Entity
@Table(name = "timetables")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Timetable extends BaseTimeEntity {

	private static final int MAX_TITLE_LENGTH = 100;
	private static final int MAX_SEMESTER_LENGTH = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
	private String title;

	@Column(name = "semester", nullable = false, length = MAX_SEMESTER_LENGTH)
	private String semester;

	@Column(name = "is_active", nullable = false)
	private boolean active;

	@Builder
	private Timetable(User user, String title, String semester) {
		validateUser(user);
		validateTitle(title);
		validateSemester(semester);
		this.user = user;
		this.title = title;
		this.semester = semester;
		this.active = false;
	}

	public void changeTitle(String title) {
		validateTitle(title);
		this.title = title;
	}

	public void changeSemester(String semester) {
		validateSemester(semester);
		this.semester = semester;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}

	private static void validateUser(User user) {
		if (user == null) {
			throw new IllegalArgumentException("user must not be null");
		}
	}

	private static void validateTitle(String title) {
		validateNotBlank(title, "title");
		validateMaxLength(title, MAX_TITLE_LENGTH, "title");
	}

	private static void validateSemester(String semester) {
		if (semester == null) {
			throw new IllegalArgumentException("semester must not be null");
		}
		validateMaxLength(semester, MAX_SEMESTER_LENGTH, "semester");
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
