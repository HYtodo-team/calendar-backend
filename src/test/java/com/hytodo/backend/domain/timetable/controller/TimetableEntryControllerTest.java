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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalTime;
import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableEntryRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryUpdateRequest;
import com.hytodo.backend.domain.timetable.service.TimetableEntryService;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import com.hytodo.backend.global.exception.GlobalExceptionHandler;
import com.hytodo.backend.global.security.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TimetableEntryControllerTest {

	private static final Long USER_ID = 1L;
	private static final Long TIMETABLE_ID = 10L;
	private static final Long ENTRY_ID = 100L;

	private MockMvc mockMvc;

	private TimetableEntryService timetableEntryService;

	@BeforeEach
	void setUp() {
		timetableEntryService = Mockito.mock(TimetableEntryService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new TimetableEntryController(timetableEntryService))
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		authenticateAs(USER_ID);
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	// 보안 필터가 없는 standalone 환경이므로, JWT 필터가 하는 일(인증 정보 저장)을 직접 흉내 낸다.
	private void authenticateAs(Long userId) {
		CustomUserDetails userDetails = new CustomUserDetails(userId);
		SecurityContextHolder.getContext().setAuthentication(
				new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
	}

	@Test
	void createReturnsCreated() throws Exception {
		given(timetableEntryService.create(eq(USER_ID), eq(TIMETABLE_ID), any(TimetableEntryRequest.class)))
				.willReturn(entryResponse(ENTRY_ID, 1, LocalTime.of(9, 0), LocalTime.of(10, 30), "자료구조"));

		mockMvc.perform(post("/api/v1/timetables/{timetableId}/entries", TIMETABLE_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dayOfWeek\":1,\"startTime\":\"09:00:00\",\"endTime\":\"10:30:00\","
								+ "\"title\":\"자료구조\",\"location\":\"공학관 101\",\"color\":\"#2563EB\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/v1/timetables/10/entries/100"))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").value(ENTRY_ID))
				.andExpect(jsonPath("$.data.dayOfWeek").value(1))
				.andExpect(jsonPath("$.data.title").value("자료구조"));
	}

	@Test
	void createSerializesTimeWithSeconds() throws Exception {
		given(timetableEntryService.create(eq(USER_ID), eq(TIMETABLE_ID), any(TimetableEntryRequest.class)))
				.willReturn(entryResponse(ENTRY_ID, 1, LocalTime.of(9, 0), LocalTime.of(10, 30), "자료구조"));

		mockMvc.perform(post("/api/v1/timetables/{timetableId}/entries", TIMETABLE_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dayOfWeek\":1,\"startTime\":\"09:00:00\",\"endTime\":\"10:30:00\","
								+ "\"title\":\"자료구조\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.startTime").value("09:00:00"))
				.andExpect(jsonPath("$.data.endTime").value("10:30:00"));
	}

	@Test
	void createRejectsInvalidFields() throws Exception {
		mockMvc.perform(post("/api/v1/timetables/{timetableId}/entries", TIMETABLE_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dayOfWeek\":8,\"startTime\":\"09:00:00\",\"endTime\":\"10:30:00\","
								+ "\"title\":\"\",\"color\":\"파랑\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

		then(timetableEntryService).should(never()).create(anyLong(), anyLong(), any());
	}

	@Test
	void createRejectsInvalidTimeRange() throws Exception {
		given(timetableEntryService.create(eq(USER_ID), eq(TIMETABLE_ID), any(TimetableEntryRequest.class)))
				.willThrow(new BusinessException(ErrorCode.INVALID_TIME_RANGE));

		mockMvc.perform(post("/api/v1/timetables/{timetableId}/entries", TIMETABLE_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"dayOfWeek\":1,\"startTime\":\"11:00:00\",\"endTime\":\"10:00:00\","
								+ "\"title\":\"자료구조\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("INVALID_TIME_RANGE"));
	}

	@Test
	void findAllReturnsEntriesInGivenOrder() throws Exception {
		given(timetableEntryService.findAll(USER_ID, TIMETABLE_ID)).willReturn(List.of(
				entryResponse(1L, 1, LocalTime.of(15, 0), LocalTime.of(16, 0), "월요일"),
				entryResponse(2L, 3, LocalTime.of(9, 0), LocalTime.of(10, 0), "수요일 오전"),
				entryResponse(3L, 3, LocalTime.of(13, 0), LocalTime.of(14, 0), "수요일 오후")));

		mockMvc.perform(get("/api/v1/timetables/{timetableId}/entries", TIMETABLE_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].title").value("월요일"))
				.andExpect(jsonPath("$.data[1].title").value("수요일 오전"))
				.andExpect(jsonPath("$.data[2].title").value("수요일 오후"))
				.andExpect(jsonPath("$.data[1].startTime").value("09:00:00"));
	}

	@Test
	void findOneReturnsEntry() throws Exception {
		given(timetableEntryService.findOne(USER_ID, TIMETABLE_ID, ENTRY_ID))
				.willReturn(entryResponse(ENTRY_ID, 1, LocalTime.of(9, 0), LocalTime.of(10, 30), "자료구조"));

		mockMvc.perform(get("/api/v1/timetables/{timetableId}/entries/{entryId}", TIMETABLE_ID, ENTRY_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(ENTRY_ID))
				.andExpect(jsonPath("$.data.startTime").value("09:00:00"));
	}

	@Test
	void findOneReturnsNotFoundForOtherUsersTimetable() throws Exception {
		Long otherUserId = 2L;
		authenticateAs(otherUserId);
		given(timetableEntryService.findOne(otherUserId, TIMETABLE_ID, ENTRY_ID))
				.willThrow(new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "시간표를 찾을 수 없습니다."));

		mockMvc.perform(get("/api/v1/timetables/{timetableId}/entries/{entryId}", TIMETABLE_ID, ENTRY_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void updateAcceptsPartialBody() throws Exception {
		given(timetableEntryService.update(eq(USER_ID), eq(TIMETABLE_ID), eq(ENTRY_ID),
				any(TimetableEntryUpdateRequest.class)))
				.willReturn(entryResponse(ENTRY_ID, 1, LocalTime.of(9, 0), LocalTime.of(10, 30), "자료구조 2분반"));

		mockMvc.perform(patch("/api/v1/timetables/{timetableId}/entries/{entryId}", TIMETABLE_ID, ENTRY_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"title\":\"자료구조 2분반\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.title").value("자료구조 2분반"))
				.andExpect(jsonPath("$.data.startTime").value("09:00:00"));
	}

	@Test
	void updateRejectsTimeWithoutPair() throws Exception {
		mockMvc.perform(patch("/api/v1/timetables/{timetableId}/entries/{entryId}", TIMETABLE_ID, ENTRY_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"startTime\":\"09:00:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

		then(timetableEntryService).should(never()).update(anyLong(), anyLong(), anyLong(), any());
	}

	@Test
	void updateRejectsEmptyBody() throws Exception {
		mockMvc.perform(patch("/api/v1/timetables/{timetableId}/entries/{entryId}", TIMETABLE_ID, ENTRY_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());

		then(timetableEntryService).should(never()).update(anyLong(), anyLong(), anyLong(), any());
	}

	@Test
	void updateRejectsInvalidTimeRange() throws Exception {
		given(timetableEntryService.update(eq(USER_ID), eq(TIMETABLE_ID), eq(ENTRY_ID),
				any(TimetableEntryUpdateRequest.class)))
				.willThrow(new BusinessException(ErrorCode.INVALID_TIME_RANGE));

		mockMvc.perform(patch("/api/v1/timetables/{timetableId}/entries/{entryId}", TIMETABLE_ID, ENTRY_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"startTime\":\"14:00:00\",\"endTime\":\"13:00:00\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorCode").value("INVALID_TIME_RANGE"));
	}

	@Test
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/timetables/{timetableId}/entries/{entryId}", TIMETABLE_ID, ENTRY_ID))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		then(timetableEntryService).should().delete(USER_ID, TIMETABLE_ID, ENTRY_ID);
	}

	private TimetableEntryResponse entryResponse(Long id, int dayOfWeek, LocalTime startTime, LocalTime endTime,
			String title) {
		return new TimetableEntryResponse(id, dayOfWeek, startTime, endTime, title, "공학관 101", null, "#2563EB");
	}
}
