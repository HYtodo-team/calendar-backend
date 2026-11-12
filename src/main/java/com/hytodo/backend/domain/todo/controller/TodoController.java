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
import com.hytodo.backend.global.security.CustomUserDetails;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TodoController {

    private final TodoService todoService;

    // API-TODO-001: Todo 생성
    @PostMapping("/todos")
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TodoCreateRequest request
    ) {
        TodoResponse response = todoService.createTodo(userDetails.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // API-TODO-002: 전체 Todo 목록 조회
    @GetMapping("/todos")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodos(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "completed", required = false) Boolean completed
    ) {
        List<TodoResponse> response = todoService.getTodos(userDetails.getUserId(), completed);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-003: Todo 상세 조회
    @GetMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> getTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "todoId") Long todoId
    ) {
        TodoResponse response = todoService.getTodo(userDetails.getUserId(), todoId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-004: 특정 일정에 연결된 Todo 목록 조회
    @GetMapping("/calendar-events/{eventId}/todos")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodosByEvent(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "eventId") Long eventId
    ) {
        List<TodoResponse> response = todoService.getTodosByEvent(userDetails.getUserId(), eventId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-005: Todo 내용 수정
    @PatchMapping("/todos/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "todoId") Long todoId,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        TodoResponse response = todoService.updateTodo(userDetails.getUserId(), todoId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-006: Todo 완료 상태 변경
    @PatchMapping("/todos/{todoId}/completion")
    public ResponseEntity<ApiResponse<TodoResponse>> updateCompletion(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "todoId") Long todoId,
            @Valid @RequestBody TodoCompletionRequest request
    ) {
        TodoResponse response = todoService.updateCompletion(userDetails.getUserId(), todoId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // API-TODO-007: Todo 삭제
    @DeleteMapping("/todos/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable(name = "todoId") Long todoId
    ) {
        todoService.deleteTodo(userDetails.getUserId(), todoId);
        return ResponseEntity.noContent().build();
    }
}