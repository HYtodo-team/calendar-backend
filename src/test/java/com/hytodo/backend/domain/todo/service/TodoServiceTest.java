package com.hytodo.backend.domain.todo.service;

import tools.jackson.databind.ObjectMapper;
import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.dto.TodoCompletionRequest;
import com.hytodo.backend.domain.todo.dto.TodoCreateRequest;
import com.hytodo.backend.domain.todo.dto.TodoResponse;
import com.hytodo.backend.domain.todo.dto.TodoUpdateRequest;
import com.hytodo.backend.domain.todo.entity.Todo;
import com.hytodo.backend.domain.todo.repository.TodoRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TodoServiceTest {

    @InjectMocks
    private TodoService todoService;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    // saveAndFlush는 Mockito에서 기본적으로 null을 반환하므로,
    // 저장된 Todo를 다시 돌려주도록 설정한다.
    @BeforeEach
    void setUp() {
        given(todoRepository.saveAndFlush(any(Todo.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("독립 투두 생성 테스트 - 성공")
    void createTodo_independent_success() {
        Long userId = 1L;
        TodoCreateRequest request =
                new TodoCreateRequest(null, "독립 투두 테스트 내용");

        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(userId);

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(100L);
        given(mockTodo.getContent()).willReturn("독립 투두 테스트 내용");
        given(mockTodo.isCompleted()).willReturn(false);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(userRepository.findById(userId))
                .willReturn(Optional.of(mockUser));
        given(todoRepository.save(any(Todo.class))).willReturn(mockTodo);

        TodoResponse response = todoService.createTodo(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.content()).isEqualTo("독립 투두 테스트 내용");
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.event()).isNull();

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());
        assertThat(captor.getValue().getEvent()).isNull();
    }

    @Test
    @DisplayName("본인 일정에 연결된 투두 생성 - 성공")
    void createTodo_withEvent_success() {
        Long userId = 1L;
        Long eventId = 10L;

        TodoCreateRequest request =
                new TodoCreateRequest(eventId, "일정 연동 투두 테스트 내용");

        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(userId);

        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(eventId);
        given(mockEvent.getTitle()).willReturn("팀 프로젝트 회의");

        Todo savedTodo = mock(Todo.class);
        given(savedTodo.getId()).willReturn(200L);
        given(savedTodo.getContent())
                .willReturn("일정 연동 투두 테스트 내용");
        given(savedTodo.isCompleted()).willReturn(false);
        given(savedTodo.getEvent()).willReturn(mockEvent);
        given(savedTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(savedTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(userRepository.findById(userId))
                .willReturn(Optional.of(mockUser));
        given(eventRepository.findByIdAndUserId(eventId, userId))
                .willReturn(Optional.of(mockEvent));
        given(todoRepository.save(any(Todo.class))).willReturn(savedTodo);

        TodoResponse response = todoService.createTodo(userId, request);

        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.content())
                .isEqualTo("일정 연동 투두 테스트 내용");
        assertThat(response.event()).isNotNull();
        assertThat(response.event().id()).isEqualTo(eventId);
        assertThat(response.event().title()).isEqualTo("팀 프로젝트 회의");

        ArgumentCaptor<Todo> captor = ArgumentCaptor.forClass(Todo.class);
        verify(todoRepository).save(captor.capture());

        assertThat(captor.getValue().getEvent()).isSameAs(mockEvent);
        assertThat(captor.getValue().getUser()).isSameAs(mockUser);
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 투두 생성 시 예외")
    void createTodo_userNotFound_throwsException() {
        Long invalidUserId = 999L;
        TodoCreateRequest request =
                new TodoCreateRequest(null, "테스트 내용");

        given(userRepository.findById(invalidUserId))
                .willReturn(Optional.empty());

        assertThatThrownBy(
                () -> todoService.createTodo(invalidUserId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("본인 소유가 아닌 일정에 투두 생성 시 예외")
    void createTodo_foreignEvent_throwsException() {
        Long userId = 1L;
        Long eventId = 999L;

        User mockUser = mock(User.class);

        TodoCreateRequest request =
                new TodoCreateRequest(eventId, "접근할 수 없는 일정");

        given(userRepository.findById(userId))
                .willReturn(Optional.of(mockUser));

        given(eventRepository.findByIdAndUserId(eventId, userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(
                () -> todoService.createTodo(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(todoRepository, never()).save(any(Todo.class));
    }

    @Test
    @DisplayName("완료 상태에 따른 전체 투두 목록 조회")
    void getTodos_completedFilter_success() {
        Long userId = 1L;

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(1L);
        given(mockTodo.getContent()).willReturn("투두 1");
        given(mockTodo.isCompleted()).willReturn(true);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(todoRepository.findAllByUserIdAndCompletedFilter(userId, true))
                .willReturn(List.of(mockTodo));

        List<TodoResponse> responses = todoService.getTodos(userId, true);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).isCompleted()).isTrue();
    }

    @Test
    @DisplayName("본인 일정에 연결된 투두 목록 실제 조회")
    void getTodosByEvent_success() {
        Long userId = 1L;
        Long eventId = 10L;

        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(eventId);
        given(mockEvent.getTitle()).willReturn("팀 회의");

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(100L);
        given(mockTodo.getContent()).willReturn("회의 자료 준비");
        given(mockTodo.getEvent()).willReturn(mockEvent);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(eventRepository.findByIdAndUserId(eventId, userId))
                .willReturn(Optional.of(mockEvent));

        given(todoRepository.findAllByUserIdAndEventId(userId, eventId))
                .willReturn(List.of(mockTodo));

        List<TodoResponse> responses =
                todoService.getTodosByEvent(userId, eventId);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(100L);
        assertThat(responses.get(0).content()).isEqualTo("회의 자료 준비");
        assertThat(responses.get(0).event().id()).isEqualTo(eventId);
        assertThat(responses.get(0).event().title()).isEqualTo("팀 회의");

        verify(todoRepository)
                .findAllByUserIdAndEventId(userId, eventId);
    }

    @Test
    @DisplayName("본인 소유가 아닌 일정의 투두 조회 차단")
    void getTodosByEvent_foreignEvent_throwsException() {
        Long userId = 1L;
        Long eventId = 999L;

        given(eventRepository.findByIdAndUserId(eventId, userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(
                () -> todoService.getTodosByEvent(userId, eventId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);

        verify(todoRepository, never())
                .findAllByUserIdAndEventId(userId, eventId);
    }

    @Test
    @DisplayName("투두 완료 상태 변경 - 성공")
    void updateCompletion_success() {
        Long userId = 1L;
        Long todoId = 100L;

        TodoCompletionRequest request = new TodoCompletionRequest(true);

        // 처음에는 미완료 상태인 실제 Todo 엔티티 사용
        Todo todo = Todo.builder()
                .user(mock(User.class))
                .content("상태 변경 테스트 내용")
                .isCompleted(false)
                .build();

        assertThat(todo.isCompleted()).isFalse();

        given(todoRepository.findByIdAndUserId(todoId, userId))
                .willReturn(Optional.of(todo));

        TodoResponse response =
                todoService.updateCompletion(userId, todoId, request);

        assertThat(response).isNotNull();
        assertThat(todo.isCompleted()).isTrue();
        assertThat(response.isCompleted()).isTrue();
        verify(todoRepository).saveAndFlush(todo);
    }

    @Test
    @DisplayName("투두 삭제 - 성공")
    void deleteTodo_success() {
        Long userId = 1L;
        Long todoId = 100L;

        Todo mockTodo = mock(Todo.class);

        given(todoRepository.findByIdAndUserId(todoId, userId))
                .willReturn(Optional.of(mockTodo));

        todoService.deleteTodo(userId, todoId);

        verify(todoRepository).delete(mockTodo);
    }

    @Test
    @DisplayName("투두의 일정 연결 해제 - 성공")
    void updateTodo_disconnectEvent_success() {
        Long userId = 1L;
        Long todoId = 100L;

        TodoUpdateRequest request =
                new TodoUpdateRequest(null, "일정 연결 해제된 투두");

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(todoId);
        given(mockTodo.getContent()).willReturn("일정 연결 해제된 투두");
        given(mockTodo.isCompleted()).willReturn(false);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(todoRepository.findByIdAndUserId(todoId, userId))
                .willReturn(Optional.of(mockTodo));

        TodoResponse response =
                todoService.updateTodo(userId, todoId, request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isEqualTo("일정 연결 해제된 투두");
        assertThat(response.event()).isNull();

        verify(mockTodo).updateEvent(null);
        verify(mockTodo).updateContent("일정 연결 해제된 투두");
    }

    @Test
    @DisplayName("투두에 본인 일정 연결 - 성공")
    void updateTodo_connectEvent_success() {
        Long userId = 1L;
        Long todoId = 100L;
        Long eventId = 10L;

        TodoUpdateRequest request =
                new TodoUpdateRequest(eventId, "연결된 투두");

        Event mockEvent = mock(Event.class);
        given(mockEvent.getId()).willReturn(eventId);
        given(mockEvent.getTitle()).willReturn("과제 발표");

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(todoId);
        given(mockTodo.getContent()).willReturn("연결된 투두");
        given(mockTodo.getEvent()).willReturn(mockEvent);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(todoRepository.findByIdAndUserId(todoId, userId))
                .willReturn(Optional.of(mockTodo));

        given(eventRepository.findByIdAndUserId(eventId, userId))
                .willReturn(Optional.of(mockEvent));

        TodoResponse response =
                todoService.updateTodo(userId, todoId, request);

        verify(mockTodo).updateEvent(mockEvent);
        verify(mockTodo).updateContent("연결된 투두");

        assertThat(response.event()).isNotNull();
        assertThat(response.event().id()).isEqualTo(eventId);
    }

    @Test
    @DisplayName("조회 목록의 정렬 순서 유지")
    void getTodos_sortingOrder_success() {
        Long userId = 1L;

        Todo mockTodo1 = mock(Todo.class);
        given(mockTodo1.getId()).willReturn(1L);
        given(mockTodo1.getContent()).willReturn("첫 번째 투두");
        given(mockTodo1.getCreatedAt())
                .willReturn(LocalDateTime.now().minusDays(1));

        Todo mockTodo2 = mock(Todo.class);
        given(mockTodo2.getId()).willReturn(2L);
        given(mockTodo2.getContent()).willReturn("두 번째 투두");
        given(mockTodo2.getCreatedAt()).willReturn(LocalDateTime.now());

        given(todoRepository.findAllByUserIdAndCompletedFilter(userId, null))
                .willReturn(List.of(mockTodo1, mockTodo2));

        List<TodoResponse> responses =
                todoService.getTodos(userId, null);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(1).id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("존재하지 않거나 본인 소유가 아닌 투두 조회 시 예외")
    void getTodo_notFound_throwsException() {
        Long userId = 1L;
        Long todoId = 999L;

        given(todoRepository.findByIdAndUserId(todoId, userId))
                .willReturn(Optional.empty());

        assertThatThrownBy(
                () -> todoService.getTodo(userId, todoId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("eventId를 생략하고 내용만 수정하면 기존 Event 연결 유지")
    void updateTodo_withoutEventId_keepsExistingEvent() throws Exception {
        Long userId = 1L;
        Long todoId = 100L;

        // 실제 JSON에서 eventId를 생략한 요청
        TodoUpdateRequest request = new ObjectMapper().readValue(
                "{\"content\":\"수정된 내용\"}",
                TodoUpdateRequest.class
        );

        User mockUser = mock(User.class);
        Event event = Event.builder()
                .user(mockUser)
                .title("기존 일정")
                .startDate(LocalDate.of(2026, 11, 6))
                .endDate(LocalDate.of(2026, 11, 6))
                .isAllDay(true)
                .isImportant(false)
                .build();

        Todo todo = Todo.builder()
                .user(mockUser)
                .event(event)
                .content("기존 내용")
                .isCompleted(false)
                .build();

        given(todoRepository.findByIdAndUserId(todoId, userId))
                .willReturn(Optional.of(todo));

        TodoResponse response = todoService.updateTodo(userId, todoId, request);

        assertThat(request.eventIdProvided()).isFalse();
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.event()).isNotNull();
        assertThat(response.event().title()).isEqualTo("기존 일정");
        assertThat(todo.getEvent()).isSameAs(event);
    }

    @Test
    @DisplayName("eventId를 명시적으로 null로 보내면 Event 연결 해제")
    void updateTodo_explicitNullEventId_disconnectsEvent() throws Exception {
        Long userId = 1L;
        Long todoId = 100L;

        // 실제 JSON에서 eventId를 명시적으로 null로 보낸 요청
        TodoUpdateRequest request = new ObjectMapper().readValue(
                "{\"eventId\":null,\"content\":\"수정된 내용\"}",
                TodoUpdateRequest.class
        );

        User mockUser = mock(User.class);
        Event event = Event.builder()
                .user(mockUser)
                .title("기존 일정")
                .startDate(LocalDate.of(2026, 11, 6))
                .endDate(LocalDate.of(2026, 11, 6))
                .isAllDay(true)
                .isImportant(false)
                .build();

        Todo todo = Todo.builder()
                .user(mockUser)
                .event(event)
                .content("기존 내용")
                .isCompleted(false)
                .build();

        given(todoRepository.findByIdAndUserId(todoId, userId))
                .willReturn(Optional.of(todo));

        TodoResponse response = todoService.updateTodo(userId, todoId, request);

        assertThat(request.eventIdProvided()).isTrue();
        assertThat(response.content()).isEqualTo("수정된 내용");
        assertThat(response.event()).isNull();
        assertThat(todo.getEvent()).isNull();
    }
}
