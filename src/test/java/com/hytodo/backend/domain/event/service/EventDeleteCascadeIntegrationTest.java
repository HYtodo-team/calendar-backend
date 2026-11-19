package com.hytodo.backend.domain.event.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.entity.Todo;
import com.hytodo.backend.domain.todo.repository.TodoRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;

/**
 * 일정 삭제 시 todos.event_id의 FK(ON DELETE CASCADE, V1 마이그레이션)로
 * 연결된 투두가 실제로 함께 삭제되는지 DB 레벨에서 검증한다.
 */
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class EventDeleteCascadeIntegrationTest {

    private static final DockerImageName MYSQL_IMAGE =
            DockerImageName.parse("mysql:8.4");

    @Container
    static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("hytodo")
            .withUsername("hytodo")
            .withPassword("1234");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", mysql::getDriverClassName);
    }

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private UserRepository userRepository;

    private EventService eventService;
    private User user;

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(
                User.builder()
                        .email("cascade@example.com")
                        .passwordHash("encoded-password")
                        .nickname("tester")
                        .build()
        );

        eventService = new EventService(eventRepository, userRepository, todoRepository);
    }

    @Test
    void deleteEventCascadesToLinkedTodosButNotToUnlinkedOnes() {
        Event event = eventRepository.save(
                Event.builder()
                        .user(user)
                        .title("삭제될 일정")
                        .startDate(LocalDate.of(2026, 9, 7))
                        .endDate(LocalDate.of(2026, 9, 7))
                        .isAllDay(true)
                        .isImportant(false)
                        .build()
        );

        Todo linkedTodo = todoRepository.save(
                Todo.builder()
                        .user(user)
                        .event(event)
                        .content("일정에 연결된 투두")
                        .isCompleted(false)
                        .build()
        );

        Todo unlinkedTodo = todoRepository.save(
                Todo.builder()
                        .user(user)
                        .content("일정과 무관한 투두")
                        .isCompleted(false)
                        .build()
        );

        eventService.deleteEvent(user.getId(), event.getId());

        assertThat(eventRepository.findById(event.getId())).isEmpty();
        assertThat(todoRepository.findById(linkedTodo.getId())).isEmpty();
        assertThat(todoRepository.findById(unlinkedTodo.getId())).isPresent();
    }
}
