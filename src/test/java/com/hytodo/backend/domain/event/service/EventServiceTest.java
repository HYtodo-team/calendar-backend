package com.hytodo.backend.domain.event.service;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

class EventServiceTest {

    private final EventRepository eventRepository =
            Mockito.mock(EventRepository.class);

    private final EventService eventService =
            new EventService(eventRepository);

    @Test
    void fromAfterToIsRejected() {
        LocalDate from = LocalDate.of(2026, 9, 20);
        LocalDate to = LocalDate.of(2026, 9, 10);

        assertThatThrownBy(() -> eventService.getEvents(1L, from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessage("INVALID_DATE_RANGE")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void rangeOver62DaysIsRejected() {
        LocalDate from = LocalDate.of(2026, 9, 1);
        LocalDate to = from.plusDays(62);

        assertThatThrownBy(() -> eventService.getEvents(1L, from, to))
                .isInstanceOf(BusinessException.class)
                .hasMessage("INVALID_DATE_RANGE")
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}