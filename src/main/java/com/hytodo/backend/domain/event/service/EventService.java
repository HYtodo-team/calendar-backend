package com.hytodo.backend.domain.event.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hytodo.backend.domain.event.dto.EventSummaryResponse;
import com.hytodo.backend.domain.event.repository.EventRepository;
import com.hytodo.backend.global.exception.BusinessException;
import com.hytodo.backend.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private static final long MAX_RANGE_DAYS = 62;

    private final EventRepository eventRepository;

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

    private void validateDateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "from and to are required"
            );
        }

        if (from.isAfter(to)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "INVALID_DATE_RANGE"
            );
        }

        long rangeDays = ChronoUnit.DAYS.between(from, to) + 1;

        if (rangeDays > MAX_RANGE_DAYS) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "INVALID_DATE_RANGE"
            );
        }
    }
}