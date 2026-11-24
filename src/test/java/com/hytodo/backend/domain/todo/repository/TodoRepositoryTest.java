
package com.hytodo.backend.domain.todo.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

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
import com.hytodo.backend.domain.todo.dto.TodoCompletionRequest;
import com.hytodo.backend.domain.todo.dto.TodoResponse;
import com.hytodo.backend.domain.todo.dto.TodoUpdateRequest;
import com.hytodo.backend.domain.todo.entity.Todo;
import com.hytodo.backend.domain.todo.service.TodoService;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
class TodoRepositoryTest {

    private static final DockerImageName MYSQL_IMAGE =
            DockerImageName.parse("mysql:8.4");

    private static final LocalDateTime PAST_TIME =
            LocalDateTime.of(2020, 1, 1, 0, 0);

    @Container
    static final MySQLContainer mysql = new MySQLContainer(MYSQL_IMAGE)
            .withDatabaseName("hytodo")
            .withUsername("hytodo")
            .withPassword("1234");

    @Autowired
    private TodoRepository todoRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TodoService todoService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;
    private User otherUser;

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name",
                mysql::getDriverClassName);
    }

    @BeforeEach
    void setUp() {
        todoRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();

        user = userRepository.save(
                User.builder()
                        .email("todo-user@example.com")
                        .passwordHash("encoded-password")
                        .nickname("todo-user")
                        .build()
        );

        otherUser = userRepository.save(
                User.builder()
                        .email("todo-other@example.com")
                        .passwordHash("encoded-password")
                        .nickname("other-user")
                        .build()
        );
    }

    // 기존 테스트 1: 특정 일정에 연결된 Todo 조회
    @Test
    void findAllByUserIdAndEventIdReturnsCorrectTodos() {
        Event myEvent = saveEvent(user, "내 일정");
        Event anotherEvent = saveEvent(user, "다른 일정");
        Event otherUsersEvent = saveEvent(otherUser, "타인 일정");

        Todo completed = saveTodo(
                user, myEvent, "완료한 투두", true);

        Todo incomplete = saveTodo(
                user, myEvent, "미완료 투두", false);

        saveTodo(user, anotherEvent, "다른 일정 투두", false);
        saveTodo(user, null, "일반 투두", false);
        saveTodo(otherUser, otherUsersEvent, "타인 투두", false);

        List<Todo> result = todoRepository.findAllByUserIdAndEventId(
                user.getId(), myEvent.getId()
        );

        assertThat(result)
                .extracting(Todo::getId)
                .containsExactly(
                        incomplete.getId(),
                        completed.getId()
                );

        assertThat(todoRepository.findAllByUserIdAndEventId(
                user.getId(), otherUsersEvent.getId()
        )).isEmpty();
    }

    // 기존 테스트 2: 다른 사용자의 Todo 조회 차단
    @Test
    void findByIdAndUserIdChecksTodoOwnership() {
        Todo todo = saveTodo(user, null, "내 투두", false);

        assertThat(todoRepository.findByIdAndUserId(
                todo.getId(), user.getId()
        )).isPresent();

        assertThat(todoRepository.findByIdAndUserId(
                todo.getId(), otherUser.getId()
        )).isEmpty();
    }

    // 기존 테스트 3: 다른 사용자의 Event 조회 차단
    @Test
    void findByIdAndUserIdChecksEventOwnership() {
        Event event = saveEvent(user, "내 일정");

        assertThat(eventRepository.findByIdAndUserId(
                event.getId(), user.getId()
        )).isPresent();

        assertThat(eventRepository.findByIdAndUserId(
                event.getId(), otherUser.getId()
        )).isEmpty();
    }

    // 기존 테스트 4: 미완료 Todo 개수 계산
    @Test
    void countIncompleteTodosByUser() {
        saveTodo(user, null, "미완료 1", false);
        saveTodo(user, null, "미완료 2", false);
        saveTodo(user, null, "완료", true);
        saveTodo(otherUser, null, "타인 미완료", false);

        long count = todoRepository.countByUserIdAndIsCompletedFalse(
                user.getId()
        );

        assertThat(count).isEqualTo(2L);
    }

    // 기존 테스트 5: 실제 DB에서 전체 목록 정렬 및 필터 검증
    @Test
    void findAllByUserIdAndCompletedFilterSortsCorrectly() {
        Todo completedOld =
                saveTodo(user, null, "오래된 완료", true);

        Todo incompleteOld =
                saveTodo(user, null, "오래된 미완료", false);

        Todo completedNew =
                saveTodo(user, null, "최근 완료", true);

        Todo incompleteNew =
                saveTodo(user, null, "최근 미완료", false);

        saveTodo(otherUser, null, "타인 투두", false);

        todoRepository.flush();

        setCreatedAt(completedOld, "2026-11-06 08:00:00");
        setCreatedAt(incompleteOld, "2026-11-06 09:00:00");
        setCreatedAt(completedNew, "2026-11-06 10:00:00");
        setCreatedAt(incompleteNew, "2026-11-06 11:00:00");

        entityManager.clear();

        List<Todo> all = todoRepository
                .findAllByUserIdAndCompletedFilter(
                        user.getId(), null
                );

        assertThat(all)
                .extracting(Todo::getId)
                .containsExactly(
                        incompleteNew.getId(),
                        incompleteOld.getId(),
                        completedNew.getId(),
                        completedOld.getId()
                );

        List<Todo> incomplete = todoRepository
                .findAllByUserIdAndCompletedFilter(
                        user.getId(), false
                );

        assertThat(incomplete)
                .extracting(Todo::getId)
                .containsExactly(
                        incompleteNew.getId(),
                        incompleteOld.getId()
                );

        List<Todo> completed = todoRepository
                .findAllByUserIdAndCompletedFilter(
                        user.getId(), true
                );

        assertThat(completed)
                .extracting(Todo::getId)
                .containsExactly(
                        completedNew.getId(),
                        completedOld.getId()
                );
    }

    // 기존 테스트 6: 생성 시 content 검증
    @Test
    void createTodoRejectsInvalidContent() {
        assertThatThrownBy(() ->
                Todo.builder()
                        .user(user)
                        .content("")
                        .isCompleted(false)
                        .build()
        ).isInstanceOf(BusinessException.class);

        assertThatThrownBy(() ->
                Todo.builder()
                        .user(user)
                        .content("   ")
                        .isCompleted(false)
                        .build()
        ).isInstanceOf(BusinessException.class);

        assertThatThrownBy(() ->
                Todo.builder()
                        .user(user)
                        .content("a".repeat(256))
                        .isCompleted(false)
                        .build()
        ).isInstanceOf(BusinessException.class);
    }

    // 기존 테스트 7: 수정 시 content 검증
    @Test
    void updateTodoRejectsInvalidContent() {
        Todo todo = saveTodo(
                user, null, "기존 내용", false
        );

        assertThatThrownBy(() ->
                todo.updateContent("")
        ).isInstanceOf(BusinessException.class);

        assertThatThrownBy(() ->
                todo.updateContent("   ")
        ).isInstanceOf(BusinessException.class);

        assertThatThrownBy(() ->
                todo.updateContent("a".repeat(256))
        ).isInstanceOf(BusinessException.class);

        assertThat(todo.getContent()).isEqualTo("기존 내용");

        todo.updateContent(null);

        assertThat(todo.getContent()).isEqualTo("기존 내용");

        todo.updateContent("수정된 내용");

        assertThat(todo.getContent()).isEqualTo("수정된 내용");
    }

    // 추가 테스트 8: 내용 수정 시 DB와 응답의 updatedAt 갱신
    @Test
    void updateTodoRefreshesUpdatedAtInResponse() {
        Todo todo = saveTodo(
                user, null, "수정 전 내용", false
        );

        setUpdatedAtToPast(todo);

        TodoResponse response = todoService.updateTodo(
                user.getId(),
                todo.getId(),
                new TodoUpdateRequest(null, "수정 후 내용")
        );

        assertThat(response.content())
                .isEqualTo("수정 후 내용");

        assertUpdatedAtWasRefreshed(todo.getId(), response);
    }

    // 추가 테스트 9: 완료 상태 변경 시 DB와 응답의 updatedAt 갱신
    @Test
    void updateCompletionRefreshesUpdatedAtInResponse() {
        Todo todo = saveTodo(
                user, null, "완료 상태 변경", false
        );

        setUpdatedAtToPast(todo);

        TodoResponse response = todoService.updateCompletion(
                user.getId(),
                todo.getId(),
                new TodoCompletionRequest(true)
        );

        assertThat(response.isCompleted()).isTrue();

        assertUpdatedAtWasRefreshed(todo.getId(), response);
    }

    private void setCreatedAt(Todo todo, String dateTime) {
        jdbcTemplate.update(
                "UPDATE todos SET created_at = ? WHERE id = ?",
                Timestamp.valueOf(dateTime),
                todo.getId()
        );
    }

    private void setUpdatedAtToPast(Todo todo) {
        jdbcTemplate.update(
                "UPDATE todos SET updated_at = ? WHERE id = ?",
                Timestamp.valueOf(PAST_TIME),
                todo.getId()
        );

        entityManager.clear();
    }

    private void assertUpdatedAtWasRefreshed(
            Long todoId,
            TodoResponse response
    ) {
        Timestamp dbTimestamp = jdbcTemplate.queryForObject(
                "SELECT updated_at FROM todos WHERE id = ?",
                Timestamp.class,
                todoId
        );

        assertThat(dbTimestamp).isNotNull();

        LocalDateTime dbUpdatedAt = dbTimestamp.toLocalDateTime();

        // DB의 과거 수정 시간이 실제로 변경됐는지 검증
        assertThat(dbUpdatedAt).isAfter(PAST_TIME);

        // API 응답에도 갱신된 수정 시간이 들어갔는지 검증
        assertThat(response.updatedAt()).isAfter(PAST_TIME);

        // DB 값과 응답 값이 시간 정밀도 차이 범위에서 일치
        assertThat(
                Duration.between(
                        dbUpdatedAt,
                        response.updatedAt()
                ).abs()
        ).isLessThan(Duration.ofSeconds(1));
    }

    private Event saveEvent(User owner, String title) {
        return eventRepository.save(
                Event.builder()
                        .user(owner)
                        .title(title)
                        .startDate(LocalDate.of(2026, 11, 6))
                        .endDate(LocalDate.of(2026, 11, 6))
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
}
