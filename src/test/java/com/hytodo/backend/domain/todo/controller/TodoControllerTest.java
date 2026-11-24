package com.hytodo.backend.domain.todo.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.hytodo.backend.domain.todo.service.TodoService;
import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import com.hytodo.backend.global.security.CustomUserDetails;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TodoControllerTest {

    private static final Long USER_ID = 1L;

    private MockMvc mockMvc;
    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = Mockito.mock(TodoService.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new TodoController(todoService))
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(USER_ID);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getTodosUsesAuthenticatedUserId() throws Exception {
        given(todoService.getTodos(USER_ID, false))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/todos")
                        .param("completed", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());

        then(todoService).should().getTodos(USER_ID, false);
    }

    @Test
    void getTodosByEventUsesAuthenticatedUserId() throws Exception {
        given(todoService.getTodosByEvent(USER_ID, 10L))
                .willReturn(List.of());

        mockMvc.perform(get("/api/v1/calendar-events/10/todos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        then(todoService).should().getTodosByEvent(USER_ID, 10L);
    }

    @Test
    void deleteTodoUsesAuthenticatedUserId() throws Exception {
        mockMvc.perform(delete("/api/v1/todos/100"))
                .andExpect(status().isNoContent());

        then(todoService).should().deleteTodo(USER_ID, 100L);
    }
}