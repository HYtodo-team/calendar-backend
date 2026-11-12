package com.hytodo.backend.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

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

import com.hytodo.backend.domain.dashboard.dto.DashboardSummaryResponse;
import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.entity.Todo;
import com.hytodo.backend.domain.todo.repository.TodoRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class DashboardServiceIntegrationTest {

    private static final LocalDate TODAY =
            LocalDate.of(2026, 10, 29);

    private static final DockerImageName MYSQL_IMAGE =
            DockerImageName.parse("mysql:8.4");

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer(MYSQL_IMAGE)
                    .withDatabaseName("hytodo")
                    .withUsername("hytodo")
                    .withPassword("1234");

    @DynamicPropertySource
    static void configureDataSource(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                mysql::getJdbcUrl
        );
        registry.add(
                "spring.datasource.username",
                mysql::getUsername
        );
        registry.add(
                "spring.datasource.password",
                mysql::getPassword
        );
        registry.add(
                "spring.datasource.driver-class-name",
                mysql::getDriverClassName
        );
    }

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private DashboardService dashboardService;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        user = saveUser("dashboard-user@example.com");
        otherUser = saveUser("dashboard-other@example.com");

        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-10-29T12:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        dashboardService = new DashboardService(
                eventRepository,
                todoRepository,
                fixedClock
        );
    }

    // 테스트 1: 고정 Clock과 오늘 일정 개수
    @Test
    void countsEventsOverlappingToday() {
        saveEvent(user, "당일 일정", TODAY, TODAY);

        saveEvent(
                user,
                "오늘 포함 기간 일정",
                TODAY.minusDays(2),
                TODAY.plusDays(2)
        );

        saveEvent(
                user,
                "어제 종료",
                TODAY.minusDays(3),
                TODAY.minusDays(1)
        );

        saveEvent(
                user,
                "내일 시작",
                TODAY.plusDays(1),
                TODAY.plusDays(2)
        );

        saveEvent(
                otherUser,
                "다른 사용자 일정",
                TODAY,
                TODAY
        );

        DashboardSummaryResponse response =
                dashboardService.getSummary(user.getId());

        assertThat(dashboardService.getToday())
                .isEqualTo(TODAY);

        assertThat(response.todayEventCount())
                .isEqualTo(2L);
    }

    // 테스트 2: 미완료 Todo만 집계하고 완료 변경 반영
    @Test
    void countsIncompleteTodosIncludingLinkedAndIndependent() {
        Event event = saveEvent(
                user,
                "연결할 일정",
                TODAY,
                TODAY
        );

        saveTodo(user, null, "독립 미완료", false);

        Todo linkedTodo = saveTodo(
                user,
                event,
                "연결 미완료",
                false
        );

        saveTodo(user, null, "완료 투두", true);

        saveTodo(
                otherUser,
                null,
                "다른 사용자 미완료",
                false
        );

        DashboardSummaryResponse before =
                dashboardService.getSummary(user.getId());

        assertThat(before.incompleteTodoCount())
                .isEqualTo(2L);

        linkedTodo.updateCompletion(true);
        todoRepository.saveAndFlush(linkedTodo);

        DashboardSummaryResponse after =
                dashboardService.getSummary(user.getId());

        assertThat(after.incompleteTodoCount())
                .isEqualTo(1L);
    }

    // 테스트 3: 임박 기간 일정 필터
    @Test
    void upcomingEventsIncludeOnlyEligiblePeriodEvents() {
        saveEvent(
                user,
                "내일 종료",
                TODAY,
                TODAY.plusDays(1)
        );

        saveEvent(
                user,
                "5일 후 종료",
                TODAY.minusDays(1),
                TODAY.plusDays(5)
        );

        saveEvent(
                user,
                "오늘 종료",
                TODAY.minusDays(2),
                TODAY
        );

        saveEvent(
                user,
                "이미 종료",
                TODAY.minusDays(5),
                TODAY.minusDays(1)
        );

        saveEvent(
                user,
                "6일 후 종료",
                TODAY,
                TODAY.plusDays(6)
        );

        saveEvent(
                user,
                "하루짜리 일정",
                TODAY.plusDays(2),
                TODAY.plusDays(2)
        );

        saveEvent(
                otherUser,
                "다른 사용자 기간 일정",
                TODAY,
                TODAY.plusDays(1)
        );

        DashboardSummaryResponse response =
                dashboardService.getSummary(user.getId());

        assertThat(response.upcomingEvents())
                .extracting(EventSummaryResponse::title)
                .containsExactly(
                        "오늘 종료",
                        "내일 종료",
                        "5일 후 종료"
                );
    }

    // 테스트 4: 종료일 우선, 동일 종료일은 생성일 순서
    @Test
    void upcomingEventsAreSortedByEndDateAndCreatedAt() {
        Event laterCreated = saveEvent(
                user,
                "늦게 생성",
                TODAY,
                TODAY.plusDays(3)
        );

        Event earlierCreated = saveEvent(
                user,
                "먼저 생성",
                TODAY,
                TODAY.plusDays(3)
        );

        saveEvent(
                user,
                "가장 빨리 종료",
                TODAY,
                TODAY.plusDays(1)
        );

        eventRepository.flush();

        setCreatedAt(
                laterCreated,
                "2026-10-20 15:00:00"
        );

        setCreatedAt(
                earlierCreated,
                "2026-10-20 09:00:00"
        );

        DashboardSummaryResponse response =
                dashboardService.getSummary(user.getId());

        assertThat(response.upcomingEvents())
                .extracting(EventSummaryResponse::title)
                .containsExactly(
                        "가장 빨리 종료",
                        "먼저 생성",
                        "늦게 생성"
                );
    }

    // 테스트 5: 데이터가 없을 때도 정상 응답
    @Test
    void emptyDashboardReturnsZeroCountsAndEmptyList() {
        DashboardSummaryResponse response =
                dashboardService.getSummary(user.getId());

        assertThat(response.todayEventCount())
                .isZero();

        assertThat(response.incompleteTodoCount())
                .isZero();

        assertThat(response.upcomingEvents())
                .isEmpty();
    }

    private User saveUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .passwordHash("encoded-password")
                        .nickname("tester")
                        .build()
        );
    }

    private Event saveEvent(
            User owner,
            String title,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return eventRepository.save(
                Event.builder()
                        .user(owner)
                        .title(title)
                        .startDate(startDate)
                        .endDate(endDate)
                        .isAllDay(true)
                        .isImportant(false)
                        .build()
        );
    }

    private Todo saveTodo(
            User owner,
            Event event,
            String content,
            boolean completed
    ) {
        return todoRepository.save(
                Todo.builder()
                        .user(owner)
                        .event(event)
                        .content(content)
                        .isCompleted(completed)
                        .build()
        );
    }

    private void setCreatedAt(
            Event event,
            String dateTime
    ) {
        jdbcTemplate.update(
                "UPDATE events SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(dateTime),
                event.getId()
        );
    }
}
