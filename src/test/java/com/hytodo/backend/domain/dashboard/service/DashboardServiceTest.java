
package com.hytodo.backend.domain.dashboard.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.hytodo.backend.domain.dashboard.dto.DashboardSummaryResponse;
import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.repository.TodoRepository;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DashboardServiceTest {

    private final EventRepository eventRepository =
            mock(EventRepository.class);

    private final TodoRepository todoRepository =
            mock(TodoRepository.class);

    private final Clock fixedClock = Clock.fixed(
            Instant.parse("2026-10-29T12:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    private final DashboardService dashboardService =
            new DashboardService(
                    eventRepository,
                    todoRepository,
                    fixedClock
            );

    // 기존 테스트: 고정 Clock으로 오늘 날짜 확인
    @Test
    void getTodayReturnsDateFromClock() {
        LocalDate today = dashboardService.getToday();

        assertThat(today)
                .isEqualTo(LocalDate.of(2026, 10, 29));
    }

    // 추가 테스트: 대시보드 요약 조회
    @Test
    void getSummaryReturnsCorrectData() {
        Long userId = 1L;

        LocalDate today = LocalDate.of(2026, 10, 29);
        LocalDate limitDate = today.plusDays(5);

        given(eventRepository.countOverlappingEventsOnDate(
                userId, today
        )).willReturn(3L);

        given(todoRepository.countByUserIdAndIsCompletedFalse(
                userId
        )).willReturn(5L);

        Event upcomingEvent = mock(Event.class);

        given(upcomingEvent.getId()).willReturn(10L);
        given(upcomingEvent.getTitle()).willReturn("다가오는 일정");
        given(upcomingEvent.getStartDate())
                .willReturn(LocalDate.of(2026, 10, 30));
        given(upcomingEvent.getEndDate())
                .willReturn(LocalDate.of(2026, 11, 2));

        given(eventRepository.findUpcomingPeriodEvents(
                userId, today, limitDate
        )).willReturn(List.of(upcomingEvent));

        DashboardSummaryResponse response =
                dashboardService.getSummary(userId);

        // 오늘 일정 개수 확인
        assertThat(response.todayEventCount())
                .isEqualTo(3L);

        // 미완료 Todo 개수 확인
        assertThat(response.incompleteTodoCount())
                .isEqualTo(5L);

        // 다가오는 일정 목록 확인
        assertThat(response.upcomingEvents())
                .hasSize(1);

        assertThat(response.upcomingEvents().get(0).id())
                .isEqualTo(10L);

        assertThat(response.upcomingEvents().get(0).title())
                .isEqualTo("다가오는 일정");

        // 로그인 사용자 ID와 날짜 범위 전달 확인
        verify(eventRepository).countOverlappingEventsOnDate(
                userId, today
        );

        verify(todoRepository).countByUserIdAndIsCompletedFalse(
                userId
        );

        verify(eventRepository).findUpcomingPeriodEvents(
                userId, today, limitDate
        );
    }
}
