package com.hytodo.backend.domain.timetable.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hytodo.backend.domain.timetable.dto.TimetableRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableDetailResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableUpdateRequest;
import com.hytodo.backend.domain.timetable.repository.TimetableRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
class TimetableServiceTest {

	private static final DockerImageName MYSQL_IMAGE = DockerImageName.parse("mysql:8.4");

	@Container
	static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
			.withDatabaseName("hytodo")
			.withUsername("hytodo")
			.withPassword("1234");

	@Autowired
	private TimetableService timetableService;

	@Autowired
	private TimetableRepository timetableRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Long userId;
	private Long otherUserId;

	@DynamicPropertySource
	static void configureDataSource(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", mysql::getJdbcUrl);
		registry.add("spring.datasource.username", mysql::getUsername);
		registry.add("spring.datasource.password", mysql::getPassword);
		registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
	}

	@BeforeEach
	void setUp() {
		timetableRepository.deleteAll();
		userRepository.deleteAll();
		userId = saveUser("owner@example.com").getId();
		otherUserId = saveUser("other@example.com").getId();
	}

	@Test
	void firstTimetableIsActivatedAutomatically() {
		TimetableResponse first = timetableService.create(userId, new TimetableRequest("1학기 시간표", "2026-1"));
		TimetableResponse second = timetableService.create(userId, new TimetableRequest("2학기 시간표", "2026-2"));

		assertThat(first.isActive()).isTrue();
		assertThat(second.isActive()).isFalse();
	}

	@Test
	void createFailsWhenUserDoesNotExist() {
		Long missingUserId = userId + otherUserId + 1;

		assertThatThrownBy(() -> timetableService.create(missingUserId, new TimetableRequest("시간표", "2026-1")))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@Test
	void findAllReturnsActiveFirstThenNewest() {
		timetableService.create(userId, new TimetableRequest("첫 번째", "2026-1"));
		timetableService.create(userId, new TimetableRequest("두 번째", "2026-2"));
		timetableService.create(userId, new TimetableRequest("세 번째", "2026-3"));

		List<TimetableResponse> timetables = timetableService.findAll(userId);

		assertThat(timetables)
				.extracting(TimetableResponse::title)
				.containsExactly("첫 번째", "세 번째", "두 번째");
	}

	@Test
	void findAllReturnsOnlyOwnTimetables() {
		timetableService.create(userId, new TimetableRequest("내 시간표", "2026-1"));
		timetableService.create(otherUserId, new TimetableRequest("남의 시간표", "2026-1"));

		assertThat(timetableService.findAll(userId))
				.extracting(TimetableResponse::title)
				.containsExactly("내 시간표");
	}

	@Test
	void findOneReturnsEmptyEntries() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();

		TimetableDetailResponse detail = timetableService.findOne(userId, timetableId);

		assertThat(detail.title()).isEqualTo("시간표");
		assertThat(detail.isActive()).isTrue();
		assertThat(detail.entries()).isEmpty();
	}

	@Test
	void findOneFailsForOtherUsersTimetable() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();

		assertThatThrownBy(() -> timetableService.findOne(otherUserId, timetableId))
				.isInstanceOf(BusinessException.class)
				.extracting(exception -> ((BusinessException) exception).getErrorCode())
				.isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
	}

	@Test
	void updateChangesOnlyGivenFields() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();

		TimetableResponse updated = timetableService.update(userId, timetableId,
				new TimetableUpdateRequest("변경된 시간표", null));

		assertThat(updated.title()).isEqualTo("변경된 시간표");
		assertThat(updated.semester()).isEqualTo("2026-1");
		assertThat(updated.isActive()).isTrue();
		assertThat(updated.updatedAt()).isAfter(updated.createdAt());
	}

	@Test
	void updateFailsForOtherUsersTimetable() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();

		assertThatThrownBy(() -> timetableService.update(otherUserId, timetableId,
				new TimetableUpdateRequest("탈취", null)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	void activateKeepsSingleActiveTimetable() {
		Long firstId = timetableService.create(userId, new TimetableRequest("첫 번째", "2026-1")).id();
		Long secondId = timetableService.create(userId, new TimetableRequest("두 번째", "2026-2")).id();

		TimetableResponse activated = timetableService.activate(userId, secondId);

		assertThat(activated.isActive()).isTrue();
		assertThat(timetableService.findOne(userId, firstId).isActive()).isFalse();
		assertThat(timetableRepository.findAllByUserIdAndActiveTrue(userId))
				.extracting(timetable -> timetable.getId())
				.containsExactly(secondId);
	}

	@Test
	void activateAlreadyActiveTimetableKeepsItActive() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();

		assertThat(timetableService.activate(userId, timetableId).isActive()).isTrue();
		assertThat(timetableRepository.findAllByUserIdAndActiveTrue(userId)).hasSize(1);
	}

	@Test
	void deleteRemovesOwnTimetable() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();

		timetableService.delete(userId, timetableId);

		assertThat(timetableRepository.findById(timetableId)).isEmpty();
	}

	@Test
	void deleteFailsForOtherUsersTimetable() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();

		assertThatThrownBy(() -> timetableService.delete(otherUserId, timetableId))
				.isInstanceOf(BusinessException.class);
		assertThat(timetableRepository.findById(timetableId)).isPresent();
	}

	@Test
	void deleteRemovesTimetableEntriesTogether() {
		Long timetableId = timetableService.create(userId, new TimetableRequest("시간표", "2026-1")).id();
		jdbcTemplate.update(
				"insert into timetable_entries (timetable_id, title, day_of_week, start_time, end_time) "
						+ "values (?, ?, ?, ?, ?)",
				timetableId, "자료구조", 1, "09:00:00", "10:30:00");

		timetableService.delete(userId, timetableId);

		Integer remaining = jdbcTemplate.queryForObject(
				"select count(*) from timetable_entries where timetable_id = ?", Integer.class, timetableId);
		assertThat(remaining).isZero();
	}

	@Test
	void concurrentActivationKeepsSingleActiveTimetable() throws Exception {
		Long firstId = timetableService.create(userId, new TimetableRequest("첫 번째", "2026-1")).id();
		Long secondId = timetableService.create(userId, new TimetableRequest("두 번째", "2026-2")).id();

		List<Long> targets = List.of(firstId, secondId);
		List<Throwable> failures = new CopyOnWriteArrayList<>();
		CountDownLatch start = new CountDownLatch(1);
		CountDownLatch done = new CountDownLatch(targets.size());
		ExecutorService executor = Executors.newFixedThreadPool(targets.size());

		try {
			for (Long targetId : targets) {
				executor.submit(() -> {
					try {
						start.await();
						timetableService.activate(userId, targetId);
					} catch (Throwable throwable) {
						failures.add(throwable);
					} finally {
						done.countDown();
					}
				});
			}
			start.countDown();
			assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
		} finally {
			executor.shutdownNow();
		}

		assertThat(failures).isEmpty();
		assertThat(timetableRepository.findAllByUserIdAndActiveTrue(userId)).hasSize(1);
	}

	private User saveUser(String email) {
		return userRepository.save(User.builder()
				.email(email)
				.passwordHash("encoded-password")
				.nickname("tester")
				.build());
	}
}
