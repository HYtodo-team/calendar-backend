package com.hytodo.backend.domain.event.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.hytodo.backend.domain.event.dto.EventCreateRequest;
import com.hytodo.backend.domain.event.dto.EventDetailResponse;
import com.hytodo.backend.domain.event.dto.EventUpdateRequest;
import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.repository.TodoRepository;
import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.domain.user.repository.UserRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class EventServiceTest {

    private final EventRepository eventRepository = Mockito.mock(EventRepository.class);
    private final UserRepository userRepository = Mockito.mock(UserRepository.class);
    private final TodoRepository todoRepository = Mockito.mock(TodoRepository.class);

    private final EventService eventService =
            new EventService(eventRepository, userRepository, todoRepository);

    @Test
    void fromAfterToIsRejected() {
        LocalDate from = LocalDate.of(2026, 9, 20);
        LocalDate to = LocalDate.of(2026, 9, 10);

        assertThatThrownBy(() -> eventService.getEvents(1L, from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_DATE_RANGE.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void rangeOver62DaysIsRejected() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = from.plusDays(62);

        assertThatThrownBy(() -> eventService.getEvents(1L, from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ErrorCode.INVALID_DATE_RANGE.getMessage())
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
    }

    @Test
    void exactly62DaysIsAllowed() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = from.plusDays(61);

        assertThatCode(() -> eventService.getEvents(1L, from, to))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("당일 종일 일정을 생성하면 시간은 null로 저장된다")
    void createEvent_allDaySameDay_success() {
        Long userId = 1L;
        User user = userOf(userId);
        LocalDate day = LocalDate.of(2026, 9, 7);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(eventRepository.save(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));

        EventCreateRequest request = new EventCreateRequest(
                "팀 회의", day, day, true, null, null, "스터디룸", null, true);

        EventDetailResponse response = eventService.createEvent(userId, request);

        assertThat(response.isAllDay()).isTrue();
        assertThat(response.startTime()).isNull();
        assertThat(response.endTime()).isNull();
    }

    @Test
    @DisplayName("당일 시간 일정을 생성하면 입력한 시간 그대로 저장된다")
    void createEvent_timedSameDay_success() {
        Long userId = 1L;
        User user = userOf(userId);
        LocalDate day = LocalDate.of(2026, 9, 7);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(eventRepository.save(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));

        EventCreateRequest request = new EventCreateRequest(
                "팀 회의", day, day, false, LocalTime.of(14, 0), LocalTime.of(15, 0),
                "스터디룸", null, true);

        EventDetailResponse response = eventService.createEvent(userId, request);

        assertThat(response.isAllDay()).isFalse();
        assertThat(response.startTime()).isEqualTo(LocalTime.of(14, 0));
        assertThat(response.endTime()).isEqualTo(LocalTime.of(15, 0));
    }

    @Test
    @DisplayName("기간 일정을 생성하면 isAllDay=false를 보내도 서버가 true로 정규화하고 시간은 null이 된다")
    void createEvent_periodEvent_normalizesToAllDay() {
        Long userId = 1L;
        User user = userOf(userId);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(eventRepository.save(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));

        EventCreateRequest request = new EventCreateRequest(
                "중간고사 기간",
                LocalDate.of(2026, 10, 19),
                LocalDate.of(2026, 10, 23),
                false,
                LocalTime.of(9, 0),
                LocalTime.of(10, 0),
                null, "시험 일정 확인", true);

        EventDetailResponse response = eventService.createEvent(userId, request);

        assertThat(response.isAllDay()).isTrue();
        assertThat(response.startTime()).isNull();
        assertThat(response.endTime()).isNull();
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 VALIDATION_ERROR(400)를 던진다")
    void createEvent_endDateBeforeStartDate_throwsValidationError() {
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(userOf(userId)));

        EventCreateRequest request = new EventCreateRequest(
                "잘못된 일정",
                LocalDate.of(2026, 9, 10),
                LocalDate.of(2026, 9, 5),
                true, null, null, null, null, false);

        assertThatThrownBy(() -> eventService.createEvent(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("비종일 당일 일정에서 startTime이 endTime보다 빠르지 않으면 INVALID_TIME_RANGE(400)를 던진다")
    void createEvent_startTimeNotBeforeEndTime_throwsInvalidTimeRange() {
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(userOf(userId)));

        LocalDate day = LocalDate.of(2026, 9, 7);
        EventCreateRequest request = new EventCreateRequest(
                "잘못된 시간", day, day, false,
                LocalTime.of(15, 0), LocalTime.of(14, 0), null, null, false);

        assertThatThrownBy(() -> eventService.createEvent(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_TIME_RANGE);
    }

    @Test
    @DisplayName("비종일 당일 일정에서 시간이 없으면 VALIDATION_ERROR(400)를 던진다")
    void createEvent_missingTimeForTimedEvent_throwsValidationError() {
        Long userId = 1L;
        given(userRepository.findById(userId)).willReturn(Optional.of(userOf(userId)));

        LocalDate day = LocalDate.of(2026, 9, 7);
        EventCreateRequest request = new EventCreateRequest(
                "시간 누락", day, day, false, null, null, null, null, false);

        assertThatThrownBy(() -> eventService.createEvent(userId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    @DisplayName("다른 사용자의 일정을 조회하면 RESOURCE_NOT_FOUND(404)를 던진다")
    void getEvent_notOwned_throwsResourceNotFound() {
        Long userId = 1L;
        Long eventId = 100L;
        given(eventRepository.findByIdAndUserId(eventId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEvent(userId, eventId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("당일 시간 일정을 기간 일정으로 수정하면 기존 시간이 제거되고 종일로 전환된다")
    void updateEvent_switchToPeriod_clearsExistingTime() {
        Long userId = 1L;
        Long eventId = 100L;
        LocalDate day = LocalDate.of(2026, 9, 7);

        Event existing = Event.builder()
                .user(userOf(userId))
                .title("팀 회의")
                .startDate(day)
                .endDate(day)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .isAllDay(false)
                .isImportant(false)
                .build();

        given(eventRepository.findByIdAndUserId(eventId, userId)).willReturn(Optional.of(existing));
        given(eventRepository.saveAndFlush(any(Event.class))).willAnswer(invocation -> invocation.getArgument(0));

        EventUpdateRequest request = new EventUpdateRequest(
                null, day, day.plusDays(3), null, null, null, null, null, null);

        EventDetailResponse response = eventService.updateEvent(userId, eventId, request);

        assertThat(response.isAllDay()).isTrue();
        assertThat(response.startTime()).isNull();
        assertThat(response.endTime()).isNull();
        assertThat(response.endDate()).isEqualTo(day.plusDays(3));
    }

    @Test
    @DisplayName("일정 삭제는 소유권 확인 후 repository.delete를 호출한다")
    void deleteEvent_delegatesToRepository() {
        Long userId = 1L;
        Long eventId = 100L;
        Event existing = Mockito.mock(Event.class);

        given(eventRepository.findByIdAndUserId(eventId, userId)).willReturn(Optional.of(existing));

        eventService.deleteEvent(userId, eventId);

        Mockito.verify(eventRepository).delete(existing);
    }

    private User userOf(Long id) {
        User user = User.builder()
                .email("user@example.com")
                .passwordHash("encoded-password")
                .nickname("tester")
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}