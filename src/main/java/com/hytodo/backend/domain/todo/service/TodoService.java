
package com.hytodo.backend.domain.todo.service;

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
    private final EventRepository eventRepository;

    // API-TODO-001: Todo 생성
    @Transactional
    public TodoResponse createTodo(Long userId, TodoCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "사용자를 찾을 수 없습니다."
                ));

        Event event = findOwnedEvent(userId, request.eventId());

        Todo todo = Todo.builder()
                .user(user)
                .event(event)
                .content(request.content())
                .isCompleted(false)
                .build();

        Todo savedTodo = todoRepository.save(todo);
        return TodoResponse.from(savedTodo);
    }

    // API-TODO-002: Todo 목록 조회
    public List<TodoResponse> getTodos(Long userId, Boolean completed) {
        return todoRepository.findAllByUserIdAndCompletedFilter(userId, completed)
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    // API-TODO-003: Todo 상세 조회
    public TodoResponse getTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> todoNotFound());

        return TodoResponse.from(todo);
    }

    // API-TODO-004: 특정 일정에 연결된 Todo 조회
    public List<TodoResponse> getTodosByEvent(Long userId, Long eventId) {
        findOwnedEvent(userId, eventId);

        return todoRepository.findAllByUserIdAndEventId(userId, eventId)
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    // API-TODO-005: Todo 내용 및 일정 연결 수정
    @Transactional
    public TodoResponse updateTodo(
            Long userId,
            Long todoId,
            TodoUpdateRequest request
    ) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> todoNotFound());

        todo.updateContent(request.content());

        // eventId가 요청에 포함된 경우에만 일정 연결 변경
        if (request.eventIdProvided()) {
            Event event = findOwnedEvent(userId, request.eventId());
            todo.updateEvent(event);
        }

        // DB에 변경사항을 반영한 후 updatedAt을 포함해 응답
        Todo savedTodo = todoRepository.saveAndFlush(todo);
        return TodoResponse.from(savedTodo);
    }

    // API-TODO-006: Todo 완료 상태 변경
    @Transactional
    public TodoResponse updateCompletion(
            Long userId,
            Long todoId,
            TodoCompletionRequest request
    ) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> todoNotFound());

        todo.updateCompletion(request.isCompleted());

        // DB에 변경사항을 반영한 후 updatedAt을 포함해 응답
        Todo savedTodo = todoRepository.saveAndFlush(todo);
        return TodoResponse.from(savedTodo);
    }

    // API-TODO-007: Todo 삭제
    @Transactional
    public void deleteTodo(Long userId, Long todoId) {
        Todo todo = todoRepository.findByIdAndUserId(todoId, userId)
                .orElseThrow(() -> todoNotFound());

        todoRepository.delete(todo);
    }

    // 본인 소유의 Event만 조회
    private Event findOwnedEvent(Long userId, Long eventId) {
        if (eventId == null) {
            return null;
        }

        return eventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "일정을 찾을 수 없습니다."
                ));
    }

    // Todo가 없거나 본인 소유가 아닐 때 공통 오류 반환
    private BusinessException todoNotFound() {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                "투두를 찾을 수 없습니다."
        );
    }
}
