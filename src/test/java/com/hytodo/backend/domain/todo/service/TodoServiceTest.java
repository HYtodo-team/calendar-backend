package com.hytodo.backend.domain.todo.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
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

    @Test
    @DisplayName("독립 투두 생성 테스트 - 성공")
    void createTodo_independent_success() {
        // given
        Long userId = 1L;
        TodoCreateRequest request = new TodoCreateRequest(null, "독립 투두 테스트 내용");

        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(userId);

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(100L);
        given(mockTodo.getContent()).willReturn("독립 투두 테스트 내용");
        given(mockTodo.isCompleted()).willReturn(false);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(todoRepository.save(any())).willReturn(mockTodo);

        // when
        TodoResponse response = todoService.createTodo(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.content()).isEqualTo("독립 투두 테스트 내용");
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.event()).isNull();
    }

    @Test
    @DisplayName("일정 연결 투두 생성 테스트 - 성공")
    void createTodo_withEvent_success() {
        // given
        Long userId = 1L;
        Long eventId = 10L;
        TodoCreateRequest request = new TodoCreateRequest(eventId, "일정 연동 투두 테스트 내용");

        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(userId);

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(200L);
        given(mockTodo.getContent()).willReturn("일정 연동 투두 테스트 내용");
        given(mockTodo.isCompleted()).willReturn(false);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
        given(todoRepository.save(any())).willReturn(mockTodo);

        // when
        TodoResponse response = todoService.createTodo(userId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(200L);
        assertThat(response.content()).isEqualTo("일정 연동 투두 테스트 내용");
        assertThat(response.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 유저인 경우 예외 발생 - 실패")
    void createTodo_userNotFound_throwsException() {
        // given
        Long invalidUserId = 999L;
        TodoCreateRequest request = new TodoCreateRequest(null, "테스트 내용");

        given(userRepository.findById(invalidUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> todoService.createTodo(invalidUserId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("전체 조회 completed 필터 테스트 - 성공")
    void getTodos_completedFilter_success() {
        // given
        Long userId = 1L;

        Todo mockTodo1 = mock(Todo.class);
        given(mockTodo1.getId()).willReturn(1L);
        given(mockTodo1.getContent()).willReturn("투두 1");
        given(mockTodo1.isCompleted()).willReturn(true);
        given(mockTodo1.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo1.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(todoRepository.findAllByUserIdAndCompletedFilter(userId, true))
                .willReturn(List.of(mockTodo1));

        // when
        List<TodoResponse> responses = todoService.getTodos(userId, true);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).isCompleted()).isTrue();
    }

    @Test
    @DisplayName("일정별 투두 조회 테스트 - 성공(임시 빈 리스트 반환)")
    void getTodosByEvent_success() {
        // given
        Long userId = 1L;
        Long eventId = 10L;

        // when
        List<TodoResponse> responses = todoService.getTodosByEvent(userId, eventId);

        // then
        assertThat(responses).isNotNull();
        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("완료 상태 변경 테스트 - 성공")
    void updateCompletion_success() {
        // given
        Long userId = 1L;
        Long todoId = 100L;
        TodoCompletionRequest request = new TodoCompletionRequest(true);

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(todoId);
        given(mockTodo.getContent()).willReturn("상태 변경 테스트 내용");
        given(mockTodo.isCompleted()).willReturn(true);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(todoRepository.findByIdAndUserId(todoId, userId)).willReturn(Optional.of(mockTodo));

        // when
        TodoResponse response = todoService.updateCompletion(userId, todoId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(todoId);
        assertThat(response.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("투두 삭제 테스트 - 성공")
    void deleteTodo_success() {
        // given
        Long userId = 1L;
        Long todoId = 100L;

        Todo mockTodo = mock(Todo.class);
        given(todoRepository.findByIdAndUserId(todoId, userId)).willReturn(Optional.of(mockTodo));

        // when
        todoService.deleteTodo(userId, todoId);

        // then
        verify(todoRepository).delete(mockTodo);
    }

    @Test
    @DisplayName("일정 연결 해제 테스트 - 성공(eventId를 null로 수정)")
    void updateTodo_disconnectEvent_success() {
        // given
        Long userId = 1L;
        Long todoId = 100L;
        TodoUpdateRequest request = new TodoUpdateRequest(null, "일정 연결 해제된 투두");

        Todo mockTodo = mock(Todo.class);
        given(mockTodo.getId()).willReturn(todoId);
        given(mockTodo.getContent()).willReturn("일정 연결 해제된 투두");
        given(mockTodo.isCompleted()).willReturn(false);
        given(mockTodo.getCreatedAt()).willReturn(LocalDateTime.now());
        given(mockTodo.getUpdatedAt()).willReturn(LocalDateTime.now());

        given(todoRepository.findByIdAndUserId(todoId, userId)).willReturn(Optional.of(mockTodo));

        // when
        TodoResponse response = todoService.updateTodo(userId, todoId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(todoId);
        assertThat(response.content()).isEqualTo("일정 연결 해제된 투두");
        assertThat(response.event()).isNull();
    }

    @Test
    @DisplayName("정렬 순서 테스트 - 성공(조회 리스트 순서 유지)")
    void getTodos_sortingOrder_success() {
        // given
        Long userId = 1L;

        Todo mockTodo1 = mock(Todo.class);
        given(mockTodo1.getId()).willReturn(1L);
        given(mockTodo1.getContent()).willReturn("첫 번째 투두");
        given(mockTodo1.getCreatedAt()).willReturn(LocalDateTime.now().minusDays(1));

        Todo mockTodo2 = mock(Todo.class);
        given(mockTodo2.getId()).willReturn(2L);
        given(mockTodo2.getContent()).willReturn("두 번째 투두");
        given(mockTodo2.getCreatedAt()).willReturn(LocalDateTime.now());

        // 순서대로 리스트 구성 (ID: 1L, ID: 2L 순)
        given(todoRepository.findAllByUserIdAndCompletedFilter(userId, null))
                .willReturn(List.of(mockTodo1, mockTodo2));

        // when
        List<TodoResponse> responses = todoService.getTodos(userId, null);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(1).id()).isEqualTo(2L);
    }
}