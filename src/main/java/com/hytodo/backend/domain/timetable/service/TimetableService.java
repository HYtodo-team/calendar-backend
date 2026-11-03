package com.hytodo.backend.domain.timetable.service;

import java.util.List;

import com.hytodo.backend.domain.timetable.dto.TimetableEntryResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableRequest;
import com.hytodo.backend.domain.timetable.dto.TimetableDetailResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableResponse;
import com.hytodo.backend.domain.timetable.dto.TimetableUpdateRequest;
import com.hytodo.backend.domain.timetable.entity.Timetable;
import com.hytodo.backend.domain.timetable.repository.TimetableEntryRepository;
import com.hytodo.backend.domain.timetable.repository.TimetableRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableService {

	private static final Sort TIMETABLE_SORT =
			Sort.by(Sort.Order.desc("active"), Sort.Order.desc("createdAt"));

	private final TimetableRepository timetableRepository;
	private final TimetableEntryRepository timetableEntryRepository;
	private final UserRepository userRepository;

	/**
	 * 시간표를 생성합니다. 활성화된 시간표가 없으면 생성된 시간표를 활성화합니다.
	 */
	@Transactional
	public TimetableResponse create(Long userId, TimetableRequest request) {
		User user = getUserForUpdate(userId);

		Timetable timetable = Timetable.builder()
				.user(user)
				.title(request.title())
				.semester(request.semester())
				.build();

		if (!timetableRepository.existsByUserIdAndActiveTrue(userId)) {
			timetable.activate();
		}

		return TimetableResponse.from(timetableRepository.save(timetable));
	}

	/**
	 * 활성화된 시간표를 앞에 두고 최신순으로 시간표 목록을 조회합니다.
	 */
	public List<TimetableResponse> findAll(Long userId) {
		return timetableRepository.findAllByUserId(userId, TIMETABLE_SORT)
				.stream()
				.map(TimetableResponse::from)
				.toList();
	}

	/**
	 * 시간표와 함께 요일·시작 시간 오름차순으로 정렬된 항목 목록을 반환합니다.
	 */
	public TimetableDetailResponse findOne(Long userId, Long timetableId) {
		Timetable timetable = getOwnedTimetable(userId, timetableId);

		List<TimetableEntryResponse> entries =
				timetableEntryRepository.findAllByTimetableIdOrderByDayOfWeekAscStartTimeAsc(timetableId)
						.stream()
						.map(TimetableEntryResponse::from)
						.toList();

		return TimetableDetailResponse.from(timetable, entries);
	}

	/**
	 * 전달된 필드만 변경하는 부분 수정입니다.
	 */
	@Transactional
	public TimetableResponse update(Long userId, Long timetableId, TimetableUpdateRequest request) {
		Timetable timetable = getOwnedTimetable(userId, timetableId);

		if (request.title() != null) {
			timetable.changeTitle(request.title());
		}
		if (request.semester() != null) {
			timetable.changeSemester(request.semester());
		}

		// 응답의 updatedAt이 변경 이전 값으로 나가지 않도록 감사 시각을 먼저 반영한다.
		return TimetableResponse.from(timetableRepository.saveAndFlush(timetable));
	}

	@Transactional
	public void delete(Long userId, Long timetableId) {
		timetableRepository.delete(getOwnedTimetable(userId, timetableId));
	}

	/**
	 * 활성화 요청이면 대상 시간표만 활성화하고 같은 사용자의 다른 시간표는 모두 비활성화합니다.
	 * 비활성화 요청이면 대상만 비활성화하며, 다른 시간표를 대신 활성화하지 않습니다.
	 * 동시 요청으로 활성 시간표가 여러 개가 되지 않도록 사용자 행을 선점한 뒤 처리합니다.
	 */
	@Transactional
	public TimetableResponse changeActivation(Long userId, Long timetableId, boolean active) {
		getUserForUpdate(userId);

		Timetable timetable = getOwnedTimetable(userId, timetableId);
		if (active) {
			timetableRepository.findAllByUserIdAndActiveTrue(userId)
					.forEach(Timetable::deactivate);
			timetable.activate();
		} else {
			timetable.deactivate();
		}

		return TimetableResponse.from(timetableRepository.saveAndFlush(timetable));
	}

	private User getUserForUpdate(Long userId) {
		return userRepository.findByIdForUpdate(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));
	}

	private Timetable getOwnedTimetable(Long userId, Long timetableId) {
		return timetableRepository.findByIdAndUserId(timetableId, userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "시간표를 찾을 수 없습니다."));
	}
}
