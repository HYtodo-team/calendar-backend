package com.hytodo.backend.domain.todo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hytodo.backend.domain.todo.dto.TodoCompletionRequest;
import com.hytodo.backend.domain.todo.dto.TodoCreateRequest;
import com.hytodo.backend.domain.todo.dto.TodoResponse;
import com.hytodo.backend.domain.todo.dto.TodoUpdateRequest;
import com.hytodo.backend.domain.todo.service.TodoService;
import com.hytodo.backend.global.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TodoController {

    private final TodoService todoService;

    // API-TODO-001: 투두 생성
    @PostMapping("/todos")
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TodoCreateRequest request
    ) {
        TodoResponse response = todoService.createTodo(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // API-TODO-002: 전체 투두 목록 조회
    @GetMapping("/todos")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodos(
            @AuthenticationPrincipal Long userId,
            @RequestParam(name = "completed", required = false) Boolean completed
    ) {
        List<TodoResponse> response = todoService.getTodos(userId, completed);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-003: 투두 상세 조회
    @GetMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> getTodo(
            @AuthenticationPrincipal Long userId,
            @PathVariable(name = "todoId") Long todoId
    ) {
        TodoResponse response = todoService.getTodo(userId, todoId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-004: 특정 일정별 투두 목록 조회
    @GetMapping("/calendar-events/{eventId}/todos")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodosByEvent(
            @AuthenticationPrincipal Long userId,
            @PathVariable(name = "eventId") Long eventId
    ) {
        List<TodoResponse> response = todoService.getTodosByEvent(userId, eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-005: 투두 내용 수정
    @PatchMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @AuthenticationPrincipal Long userId,
            @PathVariable(name = "todoId") Long todoId,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        TodoResponse response = todoService.updateTodo(userId, todoId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-006: 투두 완료 상태 변경
    @PatchMapping("/todos/{todoId}/completion")
    public ResponseEntity<ApiResponse<TodoResponse>> updateCompletion(
            @AuthenticationPrincipal Long userId,
            @PathVariable(name = "todoId") Long todoId,
            @Valid @RequestBody TodoCompletionRequest request
    ) {
        TodoResponse response = todoService.updateCompletion(userId, todoId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-007: 투두 삭제
    @DeleteMapping("/todos/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @AuthenticationPrincipal Long userId,
            @PathVariable(name = "todoId") Long todoId
    ) {
        todoService.deleteTodo(userId, todoId);
        return ResponseEntity.noContent().build();
    }
}