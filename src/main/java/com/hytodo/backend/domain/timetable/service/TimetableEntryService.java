package com.hytodo.backend.domain.timetable.service;

import java.time.LocalTime;
import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableEntryRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableEntryUpdateRequest;
import com.hytodo.backend.domain.timetable.entity.Timetable;
import com.hytodo.backend.domain.timetable.entity.TimetableEntry;
import com.hytodo.backend.domain.timetable.repository.TimetableEntryRepository;
import com.hytodo.backend.domain.timetable.repository.TimetableRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableEntryService {

	private final TimetableRepository timetableRepository;
	private final TimetableEntryRepository timetableEntryRepository;

	/**
	 * 시간표 항목을 생성합니다. 상위 시간표가 사용자 소유가 아니면 찾을 수 없는 것으로 처리합니다.
	 */
	@Transactional
	public TimetableEntryResponse create(Long userId, Long timetableId, TimetableEntryRequest request) {
		Timetable timetable = getOwnedTimetable(userId, timetableId);
		validateTimeRange(request.startTime(), request.endTime());

		TimetableEntry entry = TimetableEntry.builder()
				.timetable(timetable)
				.title(request.title())
				.dayOfWeek(request.dayOfWeek())
				.startTime(request.startTime())
				.endTime(request.endTime())
				.location(request.location())
				.memo(request.memo())
				.color(request.color())
				.build();

		return TimetableEntryResponse.from(timetableEntryRepository.save(entry));
	}

	/**
	 * 요일과 시작 시간 오름차순으로 시간표 항목 목록을 조회합니다.
	 */
	public List<TimetableEntryResponse> findAll(Long userId, Long timetableId) {
		getOwnedTimetable(userId, timetableId);

		return timetableEntryRepository.findAllByTimetableIdOrderByDayOfWeekAscStartTimeAsc(timetableId)
				.stream()
				.map(TimetableEntryResponse::from)
				.toList();
	}

	public TimetableEntryResponse findOne(Long userId, Long timetableId, Long entryId) {
		return TimetableEntryResponse.from(getOwnedEntry(userId, timetableId, entryId));
	}

	/**
	 * 전달된 필드만 변경하는 부분 수정입니다.
	 * {@code location}, {@code memo}, {@code color}는 빈 문자열을 받으면 값을 비웁니다.
	 */
	@Transactional
	public TimetableEntryResponse update(Long userId, Long timetableId, Long entryId,
			TimetableEntryUpdateRequest request) {
		TimetableEntry entry = getOwnedEntry(userId, timetableId, entryId);

		if (request.title() != null) {
			entry.changeTitle(request.title());
		}
		if (request.dayOfWeek() != null) {
			entry.changeDayOfWeek(request.dayOfWeek());
		}
		if (request.startTime() != null && request.endTime() != null) {
			validateTimeRange(request.startTime(), request.endTime());
			entry.changeTime(request.startTime(), request.endTime());
		}
		if (request.location() != null) {
			entry.changeLocation(emptyToNull(request.location()));
		}
		if (request.memo() != null) {
			entry.changeMemo(emptyToNull(request.memo()));
		}
		if (request.color() != null) {
			entry.changeColor(emptyToNull(request.color()));
		}

		return TimetableEntryResponse.from(entry);
	}

	@Transactional
	public void delete(Long userId, Long timetableId, Long entryId) {
		timetableEntryRepository.delete(getOwnedEntry(userId, timetableId, entryId));
	}

	private Timetable getOwnedTimetable(Long userId, Long timetableId) {
		return timetableRepository.findByIdAndUserId(timetableId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "시간표를 찾을 수 없습니다."));
	}

	/**
	 * 시간표 소유권을 먼저 확인한 뒤 해당 시간표에 속한 항목만 조회합니다.
	 */
	private TimetableEntry getOwnedEntry(Long userId, Long timetableId, Long entryId) {
		getOwnedTimetable(userId, timetableId);

		return timetableEntryRepository.findByIdAndTimetableId(entryId, timetableId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "시간표 항목을 찾을 수 없습니다."));
	}

	/**
	 * Entity의 IllegalArgumentException이 500으로 나가지 않도록 Service에서 먼저 검사합니다.
	 */
	private static void validateTimeRange(LocalTime startTime, LocalTime endTime) {
		if (!startTime.isBefore(endTime)) {
			throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
		}
	}

	private static String emptyToNull(String value) {
		return value.isEmpty() ? null : value;
	}
}
