package com.hytodo.backend.domain.todo.controller;

import com.hytodo.backend.domain.todo.dto.TodoCompletionRequest;
import com.hytodo.backend.domain.todo.dto.TodoCreateRequest;
import com.hytodo.backend.domain.todo.dto.TodoResponse;
import com.hytodo.backend.domain.todo.dto.TodoUpdateRequest;
import com.hytodo.backend.domain.todo.service.TodoService;
import com.hytodo.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TodoController {

    private final TodoService todoService;

    // TODO: Security 연동 전 임시 사용자 ID (1L)
    private static final Long TEMP_USER_ID = 1L;

    // API-TODO-001: 투두 생성
    @PostMapping("/todos")
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @Valid @RequestBody TodoCreateRequest request
    ) {
        TodoResponse response = todoService.createTodo(TEMP_USER_ID, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // API-TODO-002: 전체 투두 목록 조회
    @GetMapping("/todos")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodos(
            @RequestParam(name = "completed", required = false) Boolean completed
    ) {
        List<TodoResponse> response = todoService.getTodos(TEMP_USER_ID, completed);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-003: 투두 상세 조회
    @GetMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> getTodo(
            @PathVariable(name = "todoId") Long todoId
    ) {
        TodoResponse response = todoService.getTodo(TEMP_USER_ID, todoId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-004: 특정 일정별 투두 목록 조회
    @GetMapping("/calendar-events/{eventId}/todos")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodosByEvent(
            @PathVariable(name = "eventId") Long eventId
    ) {
        List<TodoResponse> response = todoService.getTodosByEvent(TEMP_USER_ID, eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-005: 투두 내용 수정
    @PatchMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @PathVariable(name = "todoId") Long todoId,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        TodoResponse response = todoService.updateTodo(TEMP_USER_ID, todoId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-006: 투두 완료 상태 변경
    @PatchMapping("/todos/{todoId}/completion")
    public ResponseEntity<ApiResponse<TodoResponse>> updateCompletion(
            @PathVariable(name = "todoId") Long todoId,
            @Valid @RequestBody TodoCompletionRequest request
    ) {
        TodoResponse response = todoService.updateCompletion(TEMP_USER_ID, todoId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-007: 투두 삭제
    @DeleteMapping("/todos/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @PathVariable(name = "todoId") Long todoId
    ) {
        todoService.deleteTodo(TEMP_USER_ID, todoId);
        return ResponseEntity.noContent().build();
    }
}