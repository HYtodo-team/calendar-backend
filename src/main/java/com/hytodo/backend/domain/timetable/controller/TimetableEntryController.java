package com.hytodo.backend.domain.timetable.controller;

import java.net.URI;
import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableEntryRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryUpdateRequest;
import com.hytodo.backend.domain.timetable.service.TimetableEntryService;
import com.hytodo.backend.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timetables/{timetableId}/entries")
@RequiredArgsConstructor
public class TimetableEntryController {

	/**
	 * 인증(JWT) 도입 전까지 사용하는 임시 사용자 식별 헤더입니다.
	 * 인증 적용 시 {@code @AuthenticationPrincipal}로 교체합니다.
	 */
	private static final String USER_ID_HEADER = "X-User-Id";

	private final TimetableEntryService timetableEntryService;

	@PostMapping
	public ResponseEntity<ApiResponse<TimetableEntryResponse>> create(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId,
			@Valid @RequestBody TimetableEntryRequest request
	) {
		TimetableEntryResponse response = timetableEntryService.create(userId, timetableId, request);
		return ResponseEntity
				.created(URI.create("/api/v1/timetables/" + timetableId + "/entries/" + response.id()))
				.body(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> findAll(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableEntryService.findAll(userId, timetableId)));
	}

	@GetMapping("/{entryId}")
	public ResponseEntity<ApiResponse<TimetableEntryResponse>> findOne(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId,
			@PathVariable Long entryId
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableEntryService.findOne(userId, timetableId, entryId)));
	}

	@PatchMapping("/{entryId}")
	public ResponseEntity<ApiResponse<TimetableEntryResponse>> update(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId,
			@PathVariable Long entryId,
			@Valid @RequestBody TimetableEntryUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(
				timetableEntryService.update(userId, timetableId, entryId, request)));
	}

	@DeleteMapping("/{entryId}")
	public ResponseEntity<Void> delete(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId,
			@PathVariable Long entryId
	) {
		timetableEntryService.delete(userId, timetableId, entryId);
		return ResponseEntity.noContent().build();
	}
}
