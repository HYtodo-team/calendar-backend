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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final UserRepository userRepository;

    // API-TODO-001: 투두 생성
    @Transactional
    public TodoResponse createTodo(Long userId, TodoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Todo todo = Todo.builder()
                .user(user)
                .content(request.content())
                .isCompleted(false)
                .build();

        Todo savedTodo = todoRepository.save(todo);
        return TodoResponse.from(savedTodo);
    }

    // API-TODO-002: 전체 투두 목록 조회
    public List<TodoResponse> getTodos(Long userId, Boolean completed) {
        return todoRepository.findAllByUserIdAndCompletedFilter(userId, completed)
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    // API-TODO-003: 투두 상세 조회
    public TodoResponse getTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));
        return TodoResponse.from(todo);
    }

    // API-TODO-004: 특정 일정별 투두 목록 조회 (Event 엔티티 연동 전 임시 빈 리스트 반환)
    public List<TodoResponse> getTodosByEvent(Long userId, Long eventId) {
        return List.of();
    }

    // API-TODO-005: 투두 내용 수정
    @Transactional
    public TodoResponse updateTodo(Long userId, Long todoId, TodoUpdateRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));

        todo.updateContent(request.content());
        return TodoResponse.from(todo);
    }

    // API-TODO-006: 투두 완료 상태 변경
    @Transactional
    public TodoResponse updateCompletion(Long userId, Long todoId, TodoCompletionRequest request) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));

        todo.updateCompletion(request.isCompleted());
        return TodoResponse.from(todo);
    }

    // API-TODO-007: 투두 삭제
    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TODO_NOT_FOUND));
        todoRepository.delete(todo);
    }
}