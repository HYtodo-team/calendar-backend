
package com.hytodo.backend.domain.dashboard.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hytodo.backend.domain.dashboard.dto.DashboardSummaryResponse;
import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.domain.todo.repository.TodoRepository;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final EventRepository eventRepository;
    private final TodoRepository todoRepository;
    private final Clock clock;

    @Autowired
    public DashboardService(
            EventRepository eventRepository,
            TodoRepository todoRepository
    ) {
        this(eventRepository, todoRepository, Clock.systemDefaultZone());
    }

    // 테스트에서 고정 Clock을 전달할 수 있는 생성자
    DashboardService(
            EventRepository eventRepository,
            TodoRepository todoRepository,
            Clock clock
    ) {
        this.eventRepository = eventRepository;
        this.todoRepository = todoRepository;
        this.clock = clock;
    }

    // 서버 기준 오늘 날짜
    public LocalDate getToday() {
        return LocalDate.now(clock);
    }

    // API-DASH-001: 대시보드 요약 조회
    public DashboardSummaryResponse getSummary(Long userId) {
        LocalDate today = getToday();
        LocalDate limitDate = today.plusDays(5);

        // 오늘 날짜와 겹치는 내 일정 개수
        long todayEventCount =
                eventRepository.countOverlappingEventsOnDate(
                        userId, today
                );

        // 독립·연결 Todo를 포함한 내 미완료 Todo 개수
        long incompleteTodoCount =
                todoRepository.countByUserIdAndIsCompletedFalse(
                        userId
                );

        // 오늘 이후 5일 이내 종료되는 내 기간 일정
        List<EventSummaryResponse> upcomingEvents =
                eventRepository.findUpcomingPeriodEvents(
                        userId, today, limitDate
                )
                .stream()
                .map(EventSummaryResponse::from)
                .toList();

        return new DashboardSummaryResponse(
                todayEventCount,
                incompleteTodoCount,
                upcomingEvents
        );
    }
}
