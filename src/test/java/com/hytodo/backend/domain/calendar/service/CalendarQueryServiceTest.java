
package com.hytodo.backend.domain.calendar.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.hytodo.backend.domain.calendar.dto.CalendarDateResponse;
import com.hytodo.backend.domain.dailynote.dto.DailyNoteResponse;
import com.hytodo.backend.domain.dailynote.service.DailyNoteService;
import com.hytodo.backend.domain.event.entity.Event;
import com.hytodo.backend.domain.event.repository.EventRepository;

class CalendarQueryServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 11, 24);

    private final EventRepository eventRepository =
            Mockito.mock(EventRepository.class);

    private final DailyNoteService dailyNoteService =
            Mockito.mock(DailyNoteService.class);

    private final CalendarQueryService calendarQueryService =
            new CalendarQueryService(eventRepository, dailyNoteService);

    @Test
    void getCalendarDate_includesSingleDayEvent() {
        Event event = createEvent("팀 회의", DATE, DATE);

        given(eventRepository.findOverlappingEvents(USER_ID, DATE, DATE))
                .willReturn(List.of(event));

        given(dailyNoteService.getNote(USER_ID, DATE))
                .willReturn(DailyNoteResponse.empty(DATE));

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(USER_ID, DATE);

        assertThat(response.date()).isEqualTo(DATE);
        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).title())
                .isEqualTo("팀 회의");

        verify(eventRepository)
                .findOverlappingEvents(USER_ID, DATE, DATE);
    }

    @Test
    void getCalendarDate_includesPeriodEvents() {
        Event event = createEvent(
                "프로젝트 기간",
                DATE.minusDays(2),
                DATE.plusDays(3)
        );

        given(eventRepository.findOverlappingEvents(USER_ID, DATE, DATE))
                .willReturn(List.of(event));

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(USER_ID, DATE);

        assertThat(response.events()).hasSize(1);
        assertThat(response.events().get(0).title())
                .isEqualTo("프로젝트 기간");
        assertThat(response.events().get(0).startDate())
                .isEqualTo(DATE.minusDays(2));
        assertThat(response.events().get(0).endDate())
                .isEqualTo(DATE.plusDays(3));
    }

    @Test
    void getCalendarDate_returnsEmptyEventsWhenNoneExist() {
        given(eventRepository.findOverlappingEvents(USER_ID, DATE, DATE))
                .willReturn(List.of());

        given(dailyNoteService.getNote(USER_ID, DATE))
                .willReturn(DailyNoteResponse.empty(DATE));

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(USER_ID, DATE);

        assertThat(response.events()).isEmpty();
        assertThat(response.dailyNote().content()).isNull();
    }

    @Test
    void getCalendarDate_includesDailyNote() {
        DailyNoteResponse note = new DailyNoteResponse(
                10L,
                DATE,
                "오늘 공부한 내용",
                LocalDateTime.of(2026, 11, 24, 12, 0)
        );

        given(eventRepository.findOverlappingEvents(USER_ID, DATE, DATE))
                .willReturn(List.of());

        given(dailyNoteService.getNote(USER_ID, DATE))
                .willReturn(note);

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(USER_ID, DATE);

        assertThat(response.dailyNote().id()).isEqualTo(10L);
        assertThat(response.dailyNote().noteDate()).isEqualTo(DATE);
        assertThat(response.dailyNote().content())
                .isEqualTo("오늘 공부한 내용");

        verify(dailyNoteService).getNote(USER_ID, DATE);
    }

    @Test
    void getCalendarDate_returnsNullContentWhenNoteMissing() {
        given(eventRepository.findOverlappingEvents(USER_ID, DATE, DATE))
                .willReturn(List.of());

        given(dailyNoteService.getNote(USER_ID, DATE))
                .willReturn(DailyNoteResponse.empty(DATE));

        CalendarDateResponse response =
                calendarQueryService.getCalendarDate(USER_ID, DATE);

        assertThat(response.dailyNote()).isNotNull();
        assertThat(response.dailyNote().noteDate()).isEqualTo(DATE);
        assertThat(response.dailyNote().content()).isNull();
    }

    private Event createEvent(
            String title,
            LocalDate startDate,
            LocalDate endDate
    ) {
        return Event.builder()
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .isAllDay(true)
                .build();
    }
}
