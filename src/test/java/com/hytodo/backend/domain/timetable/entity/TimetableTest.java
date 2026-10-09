package com.hytodo.backend.domain.timetable.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hytodo.backend.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TimetableTest {

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "\t"})
	void constructorRejectsBlankSemester(String semester) {
		assertThatThrownBy(() -> Timetable.builder()
				.user(user())
				.title("시간표")
				.semester(semester)
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("semester must not be blank");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" ", "\t"})
	void changeSemesterRejectsBlankSemester(String semester) {
		Timetable timetable = Timetable.builder()
				.user(user())
				.title("시간표")
				.semester("2026-1")
				.build();

		assertThatThrownBy(() -> timetable.changeSemester(semester))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("semester must not be blank");
	}

	@Test
	void constructorRejectsTooLongSemester() {
		assertThatThrownBy(() -> Timetable.builder()
				.user(user())
				.title("시간표")
				.semester("a".repeat(51))
				.build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("semester must be less than or equal to 50 characters");
	}

	private User user() {
		return User.builder()
				.email("owner@example.com")
				.passwordHash("encoded-password")
				.nickname("tester")
				.build();
	}
}
