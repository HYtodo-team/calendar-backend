package com.hytodo.backend.domain.event.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

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

import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class EventRepositoryTest {

    private static final DockerImageName MYSQL_IMAGE =
            DockerImageName.parse("mysql:8.4");

    @Container
    static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("hytodo")
            .withUsername("hytodo")
            .withPassword("1234");

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private User otherUser;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(
                User.builder()
                        .email("user@example.com")
                        .passwordHash("encoded-password")
                        .nickname("user")
                        .build()
        );

        otherUser = userRepository.save(
                User.builder()
                        .email("other@example.com")
                        .passwordHash("encoded-password")
                        .nickname("other")
                        .build()
        );
    }

    @Test
    void findOverlappingEventsReturnsOnlyUserEventsThatOverlapRange() {
        saveEvent(
                user,
                "inside",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 10)
        );

        saveEvent(
                user,
                "starts-before",
                LocalDate.of(2026, 9, 1),
                LocalDate.of(2026, 9, 12)
        );

        saveEvent(
                user,
                "before-range",
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 31)
        );

        saveEvent(
                user,
                "after-range",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 2)
        );

        saveEvent(
                otherUser,
                "other-user",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 10)
        );

        List<Event> result = eventRepository.findOverlappingEvents(
                user.getId(),
                LocalDate.of(2026, 9, 5),
                LocalDate.of(2026, 9, 20)
        );

        assertThat(result)
                .extracting(Event::getTitle)
                .containsExactly("starts-before", "inside");
    }

    @Test
    void eventsAreSortedByStartDateAndStartTimeWithNullLast() {
        LocalDate date = LocalDate.of(2026, 9, 10);

        saveTimedEvent(user, "afternoon", date, LocalTime.of(14, 0));
        saveTimedEvent(user, "morning", date, LocalTime.of(9, 0));
        saveEvent(user, "all-day", date, date);

        List<Event> result = eventRepository.findOverlappingEvents(
                user.getId(),
                date,
                date
        );

        assertThat(result)
                .extracting(Event::getTitle)
                .containsExactly(
                        "morning",
                        "afternoon",
                        "all-day"
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

    private Event saveTimedEvent(
            User owner,
            String title,
            LocalDate date,
            LocalTime startTime
    ) {
        return eventRepository.save(
                Event.builder()
                        .user(owner)
                        .title(title)
                        .startDate(date)
                        .endDate(date)
                        .startTime(startTime)
                        .endTime(startTime.plusHours(1))
                        .isAllDay(false)
                        .isImportant(false)
                        .build()
        );
    }
}