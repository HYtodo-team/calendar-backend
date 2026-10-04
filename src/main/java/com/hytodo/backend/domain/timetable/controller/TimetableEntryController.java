package com.hytodo.backend.domain.timetable.controller;

import java.net.URI;
import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableEntryRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryUpdateRequest;
import com.hytodo.backend.domain.timetable.service.TimetableEntryService;
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
@RequestMapping("/api/v1/timetables/{timetableId}/entries")
@RequiredArgsConstructor
public class TimetableEntryController {

	private final TimetableEntryService timetableEntryService;

	@PostMapping
	public ResponseEntity<ApiResponse<TimetableEntryResponse>> create(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId,
			@Valid @RequestBody TimetableEntryRequest request
	) {
		TimetableEntryResponse response = timetableEntryService.create(userDetails.getUserId(), timetableId, request);
		return ResponseEntity
				.created(URI.create("/api/v1/timetables/" + timetableId + "/entries/" + response.id()))
				.body(ApiResponse.success(response));
	}

	@GetMapping
	public ResponseEntity<ApiResponse<List<TimetableEntryResponse>>> findAll(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableEntryService.findAll(userDetails.getUserId(), timetableId)));
	}

	@GetMapping("/{entryId}")
	public ResponseEntity<ApiResponse<TimetableEntryResponse>> findOne(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId,
			@PathVariable Long entryId
	) {
		return ResponseEntity.ok(ApiResponse.success(timetableEntryService.findOne(userDetails.getUserId(), timetableId, entryId)));
	}

	@PatchMapping("/{entryId}")
	public ResponseEntity<ApiResponse<TimetableEntryResponse>> update(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId,
			@PathVariable Long entryId,
			@Valid @RequestBody TimetableEntryUpdateRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(
				timetableEntryService.update(userDetails.getUserId(), timetableId, entryId, request)));
	}

	@DeleteMapping("/{entryId}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal CustomUserDetails userDetails,
			@PathVariable Long timetableId,
			@PathVariable Long entryId
	) {
		timetableEntryService.delete(userDetails.getUserId(), timetableId, entryId);
		return ResponseEntity.noContent().build();
	}
}
