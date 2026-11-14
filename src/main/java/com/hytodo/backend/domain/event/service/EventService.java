package com.hytodo.backend.domain.event.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hytodo.backend.domain.event.dto.EventCreateRequest;
import com.hytodo.backend.domain.event.dto.EventDetailResponse;
import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.dto.EventUpdateRequest;
import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.dto.TodoResponse;
import com.hytodo.backend.domain.todo.repository.TodoRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final long MAX_RANGE_DAYS = 62;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final TodoRepository todoRepository;

    public List<EventSummaryResponse> getEvents(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {
        validateDateRange(from, to);

        return eventRepository.findOverlappingEvents(userId, from, to)
                .stream()
                .map(EventSummaryResponse::from)
                .toList();
    }

    // API-EVT-001: 일정 생성
    @Transactional
    public EventDetailResponse createEvent(Long userId, EventCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        ResolvedSchedule schedule = resolveSchedule(
                request.startDate(),
                request.endDate(),
                Boolean.TRUE.equals(request.isAllDay()),
                request.startTime(),
                request.endTime()
        );

        Event event = Event.builder()
                .user(user)
                .title(request.title())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .startTime(schedule.startTime())
                .endTime(schedule.endTime())
                .isAllDay(schedule.isAllDay())
                .location(request.location())
                .memo(request.memo())
                .isImportant(Boolean.TRUE.equals(request.isImportant()))
                .build();

        Event saved = eventRepository.save(event);
        return EventDetailResponse.from(saved, List.of());
    }

    // API-EVT-003: 일정 상세 및 연결 투두 조회
    public EventDetailResponse getEvent(Long userId, Long eventId) {
        Event event = getOwnedEvent(userId, eventId);
        return EventDetailResponse.from(event, getLinkedTodos(userId, eventId));
    }

    // API-EVT-004: 일정 수정
    @Transactional
    public EventDetailResponse updateEvent(Long userId, Long eventId, EventUpdateRequest request) {
        Event event = getOwnedEvent(userId, eventId);

        if (request.title() != null) {
            event.changeTitle(request.title());
        }

        if (request.startDate() != null
                || request.endDate() != null
                || request.isAllDay() != null
                || request.startTime() != null
                || request.endTime() != null) {
            applySchedule(event, request);
        }

        if (request.location() != null) {
            event.changeLocation(emptyToNull(request.location()));
        }
        if (request.memo() != null) {
            event.changeMemo(emptyToNull(request.memo()));
        }
        if (request.isImportant() != null) {
            event.changeImportant(request.isImportant());
        }

        Event saved = eventRepository.saveAndFlush(event);
        return EventDetailResponse.from(saved, getLinkedTodos(userId, eventId));
    }

    // API-EVT-005: 일정 삭제 (연결 투두는 DB cascade로 함께 삭제)
    @Transactional
    public void deleteEvent(Long userId, Long eventId) {
        eventRepository.delete(getOwnedEvent(userId, eventId));
    }

    private void applySchedule(Event event, EventUpdateRequest request) {
        LocalDate startDate = request.startDate() != null ? request.startDate() : event.getStartDate();
        LocalDate endDate = request.endDate() != null ? request.endDate() : event.getEndDate();
        boolean isAllDayRequested = request.isAllDay() != null ? request.isAllDay() : event.isAllDay();
        LocalTime startTime = request.startTime() != null ? request.startTime() : event.getStartTime();
        LocalTime endTime = request.endTime() != null ? request.endTime() : event.getEndTime();

        ResolvedSchedule schedule = resolveSchedule(startDate, endDate, isAllDayRequested, startTime, endTime);
        event.changeSchedule(startDate, endDate, schedule.isAllDay(), schedule.startTime(), schedule.endTime());
    }

    /**
     * 당일·기간 일정 정규화 정책.
     * - startDate &lt; endDate(기간 일정)이면 항상 종일·시간 NULL로 강제한다.
     * - startDate == endDate(당일 일정)이면 종일 여부에 따라 시간 필수 여부가 갈린다.
     */
    private ResolvedSchedule resolveSchedule(
            LocalDate startDate,
            LocalDate endDate,
            boolean isAllDayRequested,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "종료일은 시작일보다 빠를 수 없습니다.");
        }

        if (startDate.isBefore(endDate)) {
            return new ResolvedSchedule(true, null, null);
        }

        if (isAllDayRequested) {
            return new ResolvedSchedule(true, null, null);
        }

        if (startTime == null || endTime == null) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "종일 일정이 아니면 시작·종료 시간이 필요합니다.");
        }
        if (!startTime.isBefore(endTime)) {
            throw new BusinessException(ErrorCode.INVALID_TIME_RANGE);
        }

        return new ResolvedSchedule(false, startTime, endTime);
    }

    private Event getOwnedEvent(Long userId, Long eventId) {
        return eventRepository.findByIdAndUserId(eventId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "일정을 찾을 수 없습니다."));
    }

    private List<TodoResponse> getLinkedTodos(Long userId, Long eventId) {
        return todoRepository.findAllByUserIdAndEventId(userId, eventId)
                .stream()
                .map(TodoResponse::from)
                .toList();
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_ERROR,
                    "from and to are required"
            );
        }

        if (from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }

        long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;

        if (rangeDays > MAX_RANGE_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private record ResolvedSchedule(boolean isAllDay, LocalTime startTime, LocalTime endTime) {
    }
}