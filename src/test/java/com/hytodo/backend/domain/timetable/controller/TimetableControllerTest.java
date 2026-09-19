package com.hytodo.backend.domain.timetable.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableDetailResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableUpdateRequest;
import com.hytodo.backend.domain.timetable.service.TimetableService;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TimetableControllerTest {

	private static final String USER_ID_HEADER = "X-User-Id";
	private static final Long USER_ID = 1L;
	private static final Long TIMETABLE_ID = 10L;

	private MockMvc mockMvc;

	private TimetableService timetableService;

	@BeforeEach
	void setUp() {
		timetableService = Mockito.mock(TimetableService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new TimetableController(timetableService))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void createReturnsCreated() throws Exception {
		given(timetableService.create(eq(USER_ID), any())).willReturn(timetableResponse(true));

		mockMvc.perform(post("/api/v1/timetables")
						.header(USER_ID_HEADER, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"1학기 시간표\",\"semester\":\"2026-1\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(TIMETABLE_ID))
				.andExpect(jsonPath("$.data.isActive").value(true));
	}

	@Test
	void createRejectsBlankSemester() throws Exception {
		mockMvc.perform(post("/api/v1/timetables")
						.header(USER_ID_HEADER, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"시간표\",\"semester\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

		then(timetableService).should(never()).create(anyLong(), any());
	}

	@Test
	void createRequiresUserIdHeader() throws Exception {
		mockMvc.perform(post("/api/v1/timetables")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"시간표\",\"semester\":\"2026-1\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
	}

	@Test
	void findAllReturnsList() throws Exception {
		given(timetableService.findAll(USER_ID)).willReturn(List.of(timetableResponse(true)));

		mockMvc.perform(get("/api/v1/timetables").header(USER_ID_HEADER, USER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].isActive").value(true));
	}

	@Test
	void findOneReturnsEmptyEntries() throws Exception {
		given(timetableService.findOne(USER_ID, TIMETABLE_ID)).willReturn(new TimetableDetailResponse(
				TIMETABLE_ID, "1학기 시간표", "2026-1", true, LocalDateTime.now(), LocalDateTime.now(), List.of()));

		mockMvc.perform(get("/api/v1/timetables/{id}", TIMETABLE_ID).header(USER_ID_HEADER, USER_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.entries").isArray())
				.andExpect(jsonPath("$.data.entries").isEmpty());
	}

	@Test
	void findOneReturnsNotFound() throws Exception {
		given(timetableService.findOne(USER_ID, TIMETABLE_ID))
				.willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "시간표를 찾을 수 없습니다."));

		mockMvc.perform(get("/api/v1/timetables/{id}", TIMETABLE_ID).header(USER_ID_HEADER, USER_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void updateAcceptsPartialBody() throws Exception {
		given(timetableService.update(eq(USER_ID), eq(TIMETABLE_ID), any(TimetableUpdateRequest.class)))
				.willReturn(timetableResponse(true));

		mockMvc.perform(patch("/api/v1/timetables/{id}", TIMETABLE_ID)
						.header(USER_ID_HEADER, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"변경된 시간표\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(TIMETABLE_ID));
	}

	@Test
	void updateRejectsEmptyBody() throws Exception {
		mockMvc.perform(patch("/api/v1/timetables/{id}", TIMETABLE_ID)
						.header(USER_ID_HEADER, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		then(timetableService).should(never()).update(anyLong(), anyLong(), any());
	}

	@Test
	void activateAcceptsTrue() throws Exception {
		given(timetableService.activate(USER_ID, TIMETABLE_ID)).willReturn(timetableResponse(true));

		mockMvc.perform(patch("/api/v1/timetables/{id}/activation", TIMETABLE_ID)
						.header(USER_ID_HEADER, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"isActive\":true}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.isActive").value(true));
	}

	@Test
	void activateRejectsFalse() throws Exception {
		mockMvc.perform(patch("/api/v1/timetables/{id}/activation", TIMETABLE_ID)
						.header(USER_ID_HEADER, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"isActive\":false}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));

		then(timetableService).should(never()).activate(anyLong(), anyLong());
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/timetables/{id}", TIMETABLE_ID).header(USER_ID_HEADER, USER_ID))
				.andExpect(status().isNoContent());

		then(timetableService).should().delete(USER_ID, TIMETABLE_ID);
	}

	private TimetableResponse timetableResponse(boolean active) {
		return new TimetableResponse(
				TIMETABLE_ID, "1학기 시간표", "2026-1", active, LocalDateTime.now(), LocalDateTime.now());
	}
}
