
package com.hytodo.backend.domain.dashboard.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Date;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

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

import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.repository.TodoRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class DashboardEventCountChangeIntegrationTest {

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
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                mysql::getDriverClassName);
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

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(
                User.builder()
                        .email("dashboard-count@example.com")
                        .passwordHash("encoded-password")
                        .nickname("tester")
                        .build()
        );

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

    @Test
    void todayEventCountReflectsCreateUpdateAndDelete() {

        // 1. 일정이 없으면 0개
        assertThat(getTodayEventCount()).isZero();

        // 2. 오늘 일정 생성 -> 1개
        Event event = eventRepository.save(
                Event.builder()
                        .user(user)
                        .title("오늘 일정")
                        .startDate(TODAY)
                        .endDate(TODAY)
                        .isAllDay(true)
                        .isImportant(false)
                        .build()
        );

        assertThat(getTodayEventCount()).isEqualTo(1L);

        // 3. 일정 날짜를 내일로 변경 -> 0개
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE events
                SET start_date = ?, end_date = ?
                WHERE id = ?
                """,
                Date.valueOf(TODAY.plusDays(1)),
                Date.valueOf(TODAY.plusDays(1)),
                event.getId()
        );

        assertThat(updatedRows).isEqualTo(1);
        assertThat(getTodayEventCount()).isZero();

        // 4. 일정 날짜를 다시 오늘로 변경 -> 1개
        updatedRows = jdbcTemplate.update(
                """
                UPDATE events
                SET start_date = ?, end_date = ?
                WHERE id = ?
                """,
                Date.valueOf(TODAY),
                Date.valueOf(TODAY),
                event.getId()
        );

        assertThat(updatedRows).isEqualTo(1);
        assertThat(getTodayEventCount()).isEqualTo(1L);

        // 5. 일정 삭제 -> 0개
        eventRepository.deleteById(event.getId());

        assertThat(getTodayEventCount()).isZero();
    }

    private long getTodayEventCount() {
        return dashboardService
                .getSummary(user.getId())
                .todayEventCount();
    }
}
