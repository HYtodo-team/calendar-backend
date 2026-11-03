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
import com.hytodo.backend.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timetables")
@RequiredArgsConstructor
public class TimetableController {

	private final TimetableService timetableService;

	@PostMapping
	public ResponseEntity<ApiResponse<TimetableResponse>> create(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@Valid @RequestBody TimetableRequest request
	) {
		TimetableResponse response = timetableService.create(userDetails.getUserId(), request);
		return ResponseEntity
				.created(URI.create("/api/v1/timetables/" + response.id()))
				.body(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TimetableResponse>>> findAll(
			@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableService.findAll(userDetails.getUserId())));
	}

	@GetMapping("/{timetableId}")
	public ResponseEntity<ApiResponse<TimetableDetailResponse>> findOne(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableService.findOne(userDetails.getUserId(), timetableId)));
	}

	@PatchMapping("/{timetableId}")
	public ResponseEntity<ApiResponse<TimetableResponse>> update(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId,
			@Valid @RequestBody TimetableUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableService.update(userDetails.getUserId(), timetableId, request)));
	}

	@PatchMapping("/{timetableId}/activation")
	public ResponseEntity<ApiResponse<TimetableResponse>> changeActivation(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId,
			@Valid @RequestBody TimetableActivationRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(
				timetableService.changeActivation(userDetails.getUserId(), timetableId, request.isActive())));
	}

	@DeleteMapping("/{timetableId}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId
	) {
		timetableService.delete(userDetails.getUserId(), timetableId);
		return ResponseEntity.noContent().build();
	}
}
