
package com.hytodo.backend.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.hytodo.backend.domain.calendar.dto.CalendarDateResponse;
import com.hytodo.backend.domain.dailynote.entity.DailyNote;
import com.hytodo.backend.domain.dailynote.repository.DailyNoteRepository;
import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@Transactional
class CalendarQueryServiceIntegrationTest {

    private static final LocalDate DATE =
            LocalDate.of(2026, 11, 24);

    @Container
    static final MySQLContainer mysql =
            new MySQLContainer(DockerImageName.parse("mysql:8.4"))
                    .withDatabaseName("hytodo")
                    .withUsername("hytodo")
                    .withPassword("1234");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                mysql::getDriverClassName);
    }

    @Autowired
    private CalendarQueryService calendarQueryService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private DailyNoteRepository dailyNoteRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void selectedDateReturnsOnlyOverlappingEventsOfCurrentUser() {
        User user = createUser("mine@example.com");
        User otherUser = createUser("other@example.com");

        // 선택 날짜에 시작하는 일정
        createEvent(user, "시작일 일정",
                DATE, DATE.plusDays(2));

        // 선택 날짜에 종료되는 일정
        createEvent(user, "종료일 일정",
                DATE.minusDays(2), DATE);

        // 선택 날짜를 중간에 포함하는 일정
        createEvent(user, "기간 일정",
                DATE.minusDays(3), DATE.plusDays(3));

        // 당일 일정
        createEvent(user, "당일 일정", DATE, DATE);

        // 선택 날짜와 겹치지 않는 일정
        createEvent(user, "이전 일정",
                DATE.minusDays(3), DATE.minusDays(1));

        createEvent(user, "이후 일정",
                DATE.plusDays(1), DATE.plusDays(3));

        // 다른 사용자의 일정
        createEvent(otherUser, "다른 사용자 일정", DATE, DATE);

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(user.getId(), DATE);

        List<String> titles = response.events().stream()
                .map(EventSummaryResponse::title)
                .toList();

        assertThat(response.date()).isEqualTo(DATE);
        assertThat(titles).containsExactlyInAnyOrder(
                "시작일 일정",
                "종료일 일정",
                "기간 일정",
                "당일 일정"
        );
    }

    @Test
    void selectedDateReturnsOnlyCurrentUsersDailyNote() {
        User user = createUser("mine@example.com");
        User otherUser = createUser("other@example.com");

        createNote(user, "내 날짜 메모");
        createNote(otherUser, "다른 사용자 메모");

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(user.getId(), DATE);

        assertThat(response.dailyNote().noteDate()).isEqualTo(DATE);
        assertThat(response.dailyNote().content())
                .isEqualTo("내 날짜 메모");
        assertThat(response.events()).isEmpty();
    }

    @Test
    void selectedDateWithoutEventsOrNoteReturnsEmptyData() {
        User user = createUser("empty@example.com");

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(user.getId(), DATE);

        assertThat(response.date()).isEqualTo(DATE);
        assertThat(response.events()).isEmpty();

        assertThat(response.dailyNote()).isNotNull();
        assertThat(response.dailyNote().noteDate()).isEqualTo(DATE);
        assertThat(response.dailyNote().id()).isNull();
        assertThat(response.dailyNote().content()).isNull();
        assertThat(response.dailyNote().updatedAt()).isNull();
    }

    private User createUser(String email) {
        return userRepository.save(
                User.builder()
                        .email(email)
                        .passwordHash("encoded-password")
                        .nickname("tester")
                        .build()
        );
    }

    private Event createEvent(
            User user,
            String title,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return eventRepository.save(
                Event.builder()
                        .user(user)
                        .title(title)
                        .startDate(startDate)
                        .endDate(endDate)
                        .isAllDay(true)
                        .isImportant(false)
                        .build()
        );
    }

    private DailyNote createNote(User user, String content) {
        return dailyNoteRepository.save(
                DailyNote.builder()
                        .user(user)
                        .noteDate(DATE)
                        .content(content)
                        .build()
        );
    }
}
