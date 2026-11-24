
package com.hytodo.backend.domain.calendar.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hytodo.backend.domain.calendar.dto.CalendarDateResponse;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.dailynote.service.DailyNoteService;
import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.repository.EventRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CalendarQueryService {

    private final EventRepository eventRepository;
    private final DailyNoteService dailyNoteService;

    public CalendarDateResponse getCalendarDate(Long userId, LocalDate date) {

        // 1. 선택한 날짜와 겹치는 일정 조회
        List<EventSummaryResponse> events =
                eventRepository.findOverlappingEvents(userId, date, date)
                        .stream()
                        .map(EventSummaryResponse::from)
                        .toList();

        // 2. 선택한 날짜의 메모 조회
        DailyNoteResponse dailyNote =
                dailyNoteService.getNote(userId, date);

        // 3. 일정과 메모를 하나의 응답으로 반환
        return new CalendarDateResponse(date, events, dailyNote);
    }
}
