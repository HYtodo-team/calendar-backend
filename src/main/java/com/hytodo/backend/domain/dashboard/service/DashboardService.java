package com.hytodo.backend.domain.dashboard.service;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final Clock clock;

    public DashboardService() {
        this(Clock.systemDefaultZone());
    }

    DashboardService(Clock clock) {
        this.clock = clock;
    }

    public LocalDate getToday() {
        return LocalDate.now(clock);
    }
}