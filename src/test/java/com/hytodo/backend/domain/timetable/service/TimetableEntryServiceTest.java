package com.hytodo.backend.domain.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableEntryRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryUpdateRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableRequest;
import com.hytodo.backend.domain.timetable.repository.TimetableEntryRepository;
import com.hytodo.backend.domain.timetable.repository.TimetableRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
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
class TimetableEntryServiceTest {

	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	@Container
	static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
			.withDatabaseName("hytodo")
			.withUsername("hytodo")
			.withPassword("1234");

	@Autowired
	private TimetableEntryService timetableEntryService;

	@Autowired
	private TimetableService timetableService;

	@Autowired
	private TimetableEntryRepository timetableEntryRepository;

	@Autowired
	private TimetableRepository timetableRepository;

	@Autowired
	private UserRepository userRepository;

	private Long userId;
	private Long otherUserId;
	private Long timetableId;
	private Long otherTimetableId;

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

		userId = saveUser("owner@example.com").getId();
		otherUserId = saveUser("other@example.com").getId();
		timetableId = timetableService.create(userId, new TimetableRequest("2026-1", "2026-1")).id();
		otherTimetableId = timetableService.create(userId, new TimetableRequest("2026-2", "2026-2")).id();
	}

	@Test
	void createReturnsSavedValues() {
		TimetableEntryResponse created = timetableEntryService.create(userId, timetableId,
				new TimetableEntryRequest(1, LocalTime.of(9, 0), LocalTime.of(10, 30), "Algorithms",
						"Engineering Hall 101", "bring laptop", "#2563EB"));

		assertThat(created.id()).isNotNull();
		assertThat(created.dayOfWeek()).isEqualTo(1);
		assertThat(created.startTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(created.endTime()).isEqualTo(LocalTime.of(10, 30));
		assertThat(created.title()).isEqualTo("Algorithms");
		assertThat(created.location()).isEqualTo("Engineering Hall 101");
		assertThat(created.memo()).isEqualTo("bring laptop");
		assertThat(created.color()).isEqualTo("#2563EB");
	}

	@Test
	void createFailsForOtherUsersTimetable() {
		assertThatThrownBy(() -> timetableEntryService.create(otherUserId, timetableId, request(1, 9, 10, "Algorithms")))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@Test
	void createFailsWhenStartTimeIsNotBeforeEndTime() {
		assertThatThrownBy(() -> timetableEntryService.create(userId, timetableId,
				new TimetableEntryRequest(1, LocalTime.of(10, 0), LocalTime.of(10, 0), "Algorithms", null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_TIME_RANGE);
	}

	@Test
	void findAllOrdersByDayOfWeekThenStartTime() {
		timetableEntryService.create(userId, timetableId, request(3, 13, 14, "Wednesday second"));
		timetableEntryService.create(userId, timetableId, request(1, 15, 16, "Monday"));
		timetableEntryService.create(userId, timetableId, request(3, 9, 10, "Wednesday first"));

		List<TimetableEntryResponse> entries = timetableEntryService.findAll(userId, timetableId);

		assertThat(entries)
				.extracting(TimetableEntryResponse::title)
				.containsExactly("Monday", "Wednesday first", "Wednesday second");
	}

	@Test
	void findAllReturnsOnlyRequestedTimetableEntries() {
		timetableEntryService.create(userId, timetableId, request(1, 9, 10, "mine"));
		timetableEntryService.create(userId, otherTimetableId, request(1, 9, 10, "other timetable"));

		assertThat(timetableEntryService.findAll(userId, timetableId))
				.extracting(TimetableEntryResponse::title)
				.containsExactly("mine");
	}

	@Test
	void findOneReturnsEntry() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(2, 9, 11, "Database")).id();

		TimetableEntryResponse found = timetableEntryService.findOne(userId, timetableId, entryId);

		assertThat(found.id()).isEqualTo(entryId);
		assertThat(found.title()).isEqualTo("Database");
		assertThat(found.startTime()).isEqualTo(LocalTime.of(9, 0));
	}

	@Test
	void findOneFailsForOtherUsersTimetable() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(1, 9, 10, "Algorithms")).id();

		assertThatThrownBy(() -> timetableEntryService.findOne(otherUserId, timetableId, entryId))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@Test
	void findOneFailsForEntryOfAnotherTimetable() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(1, 9, 10, "Algorithms")).id();

		assertThatThrownBy(() -> timetableEntryService.findOne(userId, otherTimetableId, entryId))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@Test
	void updateChangesOnlyGivenFields() {
		Long entryId = timetableEntryService.create(userId, timetableId,
				new TimetableEntryRequest(1, LocalTime.of(9, 0), LocalTime.of(10, 30), "Algorithms",
						"Engineering Hall 101", "bring laptop", "#2563EB")).id();

		TimetableEntryResponse updated = timetableEntryService.update(userId, timetableId, entryId,
				new TimetableEntryUpdateRequest(null, null, null, "Algorithms II", null, null, null));

		assertThat(updated.title()).isEqualTo("Algorithms II");
		assertThat(updated.dayOfWeek()).isEqualTo(1);
		assertThat(updated.startTime()).isEqualTo(LocalTime.of(9, 0));
		assertThat(updated.endTime()).isEqualTo(LocalTime.of(10, 30));
		assertThat(updated.location()).isEqualTo("Engineering Hall 101");
		assertThat(updated.memo()).isEqualTo("bring laptop");
		assertThat(updated.color()).isEqualTo("#2563EB");
	}

	@Test
	void updateWithEmptyStringClearsOptionalFields() {
		Long entryId = timetableEntryService.create(userId, timetableId,
				new TimetableEntryRequest(1, LocalTime.of(9, 0), LocalTime.of(10, 30), "Algorithms",
						"Engineering Hall 101", "bring laptop", "#2563EB")).id();

		timetableEntryService.update(userId, timetableId, entryId,
				new TimetableEntryUpdateRequest(null, null, null, null, "", "", ""));

		TimetableEntryResponse found = timetableEntryService.findOne(userId, timetableId, entryId);
		assertThat(found.location()).isNull();
		assertThat(found.memo()).isNull();
		assertThat(found.color()).isNull();
		assertThat(found.title()).isEqualTo("Algorithms");
	}

	@Test
	void updateChangesTimeRangeTogether() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(1, 9, 10, "Algorithms")).id();

		TimetableEntryResponse updated = timetableEntryService.update(userId, timetableId, entryId,
				new TimetableEntryUpdateRequest(null, LocalTime.of(13, 0), LocalTime.of(14, 30), null, null, null,
						null));

		assertThat(updated.startTime()).isEqualTo(LocalTime.of(13, 0));
		assertThat(updated.endTime()).isEqualTo(LocalTime.of(14, 30));
	}

	@Test
	void updateFailsWhenStartTimeIsNotBeforeEndTime() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(1, 9, 10, "Algorithms")).id();

		assertThatThrownBy(() -> timetableEntryService.update(userId, timetableId, entryId,
				new TimetableEntryUpdateRequest(null, LocalTime.of(14, 0), LocalTime.of(13, 0), null, null, null,
						null)))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_TIME_RANGE);
	}

	@Test
	void updateFailsForOtherUsersTimetable() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(1, 9, 10, "Algorithms")).id();

		assertThatThrownBy(() -> timetableEntryService.update(otherUserId, timetableId, entryId,
				new TimetableEntryUpdateRequest(null, null, null, "changed", null, null, null)))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@Test
	void deleteRemovesEntry() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(1, 9, 10, "Algorithms")).id();

		timetableEntryService.delete(userId, timetableId, entryId);

		assertThatThrownBy(() -> timetableEntryService.findOne(userId, timetableId, entryId))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@Test
	void deleteFailsForOtherUsersTimetable() {
		Long entryId = timetableEntryService.create(userId, timetableId, request(1, 9, 10, "Algorithms")).id();

		assertThatThrownBy(() -> timetableEntryService.delete(otherUserId, timetableId, entryId))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
		assertThat(timetableEntryRepository.findById(entryId)).isPresent();
	}

	private TimetableEntryRequest request(int dayOfWeek, int startHour, int endHour, String title) {
		return new TimetableEntryRequest(dayOfWeek, LocalTime.of(startHour, 0), LocalTime.of(endHour, 0), title,
				null, null, null);
	}

	private User saveUser(String email) {
		return userRepository.save(User.builder()
				.email(email)
				.passwordHash("encoded-password")
				.nickname("tester")
				.build());
	}
}
