package com.hytodo.backend.domain.timetable.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;

import com.hytodo.backend.domain.timetable.entity.Timetable;
import com.hytodo.backend.domain.timetable.entity.TimetableEntry;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class TimetableEntryRepositoryTest {

	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	@Container
	static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
			.withDatabaseName("hytodo")
			.withUsername("hytodo")
			.withPassword("1234");

	@Autowired
	private TimetableEntryRepository timetableEntryRepository;

	@Autowired
	private TimetableRepository timetableRepository;

	@Autowired
	private UserRepository userRepository;

	private Timetable timetable;
	private Timetable otherTimetable;

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
	}

	@BeforeEach
	void setUp() {
		timetableEntryRepository.deleteAll();
		timetableRepository.deleteAll();
		userRepository.deleteAll();

		User user = userRepository.save(User.builder()
				.email("owner@example.com")
				.passwordHash("encoded-password")
				.nickname("owner")
				.build());
		timetable = timetableRepository.save(Timetable.builder()
				.user(user)
				.title("2026-1 timetable")
				.semester("2026-1")
				.build());
		otherTimetable = timetableRepository.save(Timetable.builder()
				.user(user)
				.title("2026-2 timetable")
				.semester("2026-2")
				.build());
	}

	@Test
	void saveAndFindKeepsAllFields() {
		TimetableEntry saved = timetableEntryRepository.saveAndFlush(TimetableEntry.builder()
				.timetable(timetable)
				.title("Algorithms")
				.dayOfWeek(3)
				.startTime(LocalTime.of(9, 0))
				.endTime(LocalTime.of(10, 30))
				.location("Engineering Hall 301")
				.memo("bring laptop")
				.color("#FF5733")
				.build());

		TimetableEntry found = timetableEntryRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getTitle()).isEqualTo("Algorithms");
		assertThat(found.getDayOfWeek()).isEqualTo(3);
		assertThat(found.getStartTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(found.getEndTime()).isEqualTo(LocalTime.of(10, 30));
		assertThat(found.getLocation()).isEqualTo("Engineering Hall 301");
		assertThat(found.getMemo()).isEqualTo("bring laptop");
		assertThat(found.getColor()).isEqualTo("#FF5733");
		assertThat(found.getTimetable().getId()).isEqualTo(timetable.getId());
	}

	@Test
	void findAllOrdersByDayOfWeekThenStartTime() {
		saveEntry(timetable, "Wednesday second", 3, LocalTime.of(13, 0), LocalTime.of(14, 0));
		saveEntry(timetable, "Monday", 1, LocalTime.of(15, 0), LocalTime.of(16, 0));
		saveEntry(timetable, "Wednesday first", 3, LocalTime.of(9, 0), LocalTime.of(10, 0));

		List<TimetableEntry> entries =
				timetableEntryRepository.findAllByTimetableIdOrderByDayOfWeekAscStartTimeAsc(timetable.getId());

		assertThat(entries)
				.extracting(TimetableEntry::getTitle)
				.containsExactly("Monday", "Wednesday first", "Wednesday second");
	}

	@Test
	void findAllReturnsOnlyRequestedTimetableEntries() {
		saveEntry(timetable, "mine", 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
		saveEntry(otherTimetable, "not mine", 1, LocalTime.of(9, 0), LocalTime.of(10, 0));

		List<TimetableEntry> entries =
				timetableEntryRepository.findAllByTimetableIdOrderByDayOfWeekAscStartTimeAsc(timetable.getId());

		assertThat(entries)
				.extracting(TimetableEntry::getTitle)
				.containsExactly("mine");
	}

	@Test
	void findByIdAndTimetableIdIsEmptyForAnotherTimetable() {
		TimetableEntry entry = saveEntry(timetable, "mine", 1, LocalTime.of(9, 0), LocalTime.of(10, 0));

		assertThat(timetableEntryRepository.findByIdAndTimetableId(entry.getId(), timetable.getId()))
				.isPresent();
		assertThat(timetableEntryRepository.findByIdAndTimetableId(entry.getId(), otherTimetable.getId()))
				.isEmpty();
	}

	@Test
	void saveAllowsNullOptionalFields() {
		TimetableEntry saved = timetableEntryRepository.saveAndFlush(TimetableEntry.builder()
				.timetable(timetable)
				.title("Database")
				.dayOfWeek(5)
				.startTime(LocalTime.of(18, 0))
				.endTime(LocalTime.of(19, 50))
				.build());

		TimetableEntry found = timetableEntryRepository.findById(saved.getId()).orElseThrow();

		assertThat(found.getLocation()).isNull();
		assertThat(found.getMemo()).isNull();
		assertThat(found.getColor()).isNull();
		assertThat(found.getStartTime()).isEqualTo(LocalTime.of(18, 0));
	}

	private TimetableEntry saveEntry(Timetable target, String title, int dayOfWeek, LocalTime startTime,
			LocalTime endTime) {
		return timetableEntryRepository.saveAndFlush(TimetableEntry.builder()
				.timetable(target)
				.title(title)
				.dayOfWeek(dayOfWeek)
				.startTime(startTime)
				.endTime(endTime)
				.build());
	}
}
