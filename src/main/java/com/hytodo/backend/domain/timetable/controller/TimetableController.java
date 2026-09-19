package com.hytodo.backend.domain.timetable.controller;

import java.net.URI;
import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableActivationRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableDetailResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableUpdateRequest;
import com.hytodo.backend.domain.timetable.service.TimetableService;
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
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
public class TimetableController {

	/**
	 * 인증(JWT) 도입 전까지 사용하는 임시 사용자 식별 헤더입니다.
	 * 인증 적용 시 {@code @AuthenticationPrincipal}로 교체합니다.
	 */
	private static final String USER_ID_HEADER = "X-User-Id";

	private final TimetableService timetableService;

	@PostMapping
	public ResponseEntity<ApiResponse<TimetableResponse>> create(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@Valid @RequestBody TimetableRequest request
	) {
		TimetableResponse response = timetableService.create(userId, request);
		return ResponseEntity
				.created(URI.create("/api/v1/timetables/" + response.id()))
				.body(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TimetableResponse>>> findAll(
			@RequestHeader(USER_ID_HEADER) Long userId
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableService.findAll(userId)));
	}

	@GetMapping("/{timetableId}")
	public ResponseEntity<ApiResponse<TimetableDetailResponse>> findOne(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableService.findOne(userId, timetableId)));
	}

	@PatchMapping("/{timetableId}")
	public ResponseEntity<ApiResponse<TimetableResponse>> update(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId,
			@Valid @RequestBody TimetableUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableService.update(userId, timetableId, request)));
	}

	@PatchMapping("/{timetableId}/activation")
	public ResponseEntity<ApiResponse<TimetableResponse>> activate(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId,
			@Valid @RequestBody TimetableActivationRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableService.activate(userId, timetableId)));
	}

	@DeleteMapping("/{timetableId}")
	public ResponseEntity<Void> delete(
			@RequestHeader(USER_ID_HEADER) Long userId,
			@PathVariable Long timetableId
	) {
		timetableService.delete(userId, timetableId);
		return ResponseEntity.noContent().build();
	}
}
