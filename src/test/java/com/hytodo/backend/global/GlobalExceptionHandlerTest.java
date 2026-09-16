package com.hytodo.backend.global;

import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import com.hytodo.backend.global.exception.OwnershipValidator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;


class GlobalExceptionHandlerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("validation 실패 시 VALIDATION_ERROR와 fieldErrors를 반환한다")
    void validationFailure() throws Exception {
        mockMvc.perform(post("/test/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("title"));
    }

    @Test
    @DisplayName("존재하지 않는 리소스는 404 RESOURCE_NOT_FOUND를 반환한다")
    void resourceNotFound() throws Exception {
        mockMvc.perform(get("/test/events/{eventId}", 999L).param("currentUserId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    @DisplayName("타 사용자 소유 리소스도 존재하지 않는 경우와 동일하게 404를 반환한다")
    void othersResource_sameAs404() throws Exception {
        String othersBody = mockMvc.perform(get("/test/events/{eventId}", 5L).param("currentUserId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andReturn().getResponse().getContentAsString();

        String missingBody = mockMvc.perform(get("/test/events/{eventId}", 999L).param("currentUserId", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"))
                .andReturn().getResponse().getContentAsString();

        assertThat(othersBody).contains("RESOURCE_NOT_FOUND");
        assertThat(missingBody).contains("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("알 수 없는 예외는 500을 반환하고 내부 메시지를 노출하지 않는다")
    void unexpectedException() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_SERVER_ERROR"))
                .andExpect(content().string(not(containsString("should never leak this"))));
    }

    @RestController
    static class TestController {

        @PostMapping("/test/events")
        public String create(@Valid @RequestBody TestRequest request) {
            return "ok";
        }

        @GetMapping("/test/events/{eventId}")
        public String getEvent(@PathVariable Long eventId, @RequestParam Long currentUserId) {
            Long ownerId = eventId.equals(5L) ? 2L : null; // 5=타인 소유, 999=미존재
            OwnershipValidator.validate(
                    Optional.ofNullable(ownerId == null ? null : eventId),
                    id -> ownerId,
                    currentUserId,
                    "일정");
            return "event-" + eventId;
        }

        @GetMapping("/test/boom")
        public String boom() {
            throw new IllegalStateException("should never leak this internal detail");
        }
    }

    static class TestRequest {
        @NotBlank(message = "제목은 필수입니다.")
        private String title;
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
    }
}
