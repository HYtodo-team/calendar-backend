package com.hytodo.backend.domain.dashboard.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    @Test
    void getTodayReturnsDateFromClock() {
        Clock fixedClock = Clock.fixed(
                Instant.parse("2026-10-29T12:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );

        DashboardService dashboardService =
                new DashboardService(fixedClock);

        LocalDate today = dashboardService.getToday();

        assertThat(today).isEqualTo(LocalDate.of(2026, 10, 29));
    }
}