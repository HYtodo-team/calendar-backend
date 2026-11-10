
package com.hytodo.backend.domain.event.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hytodo.backend.domain.event.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

    // BE-05: 기간별 일정 조회
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.user.id = :userId
              AND e.startDate <= :to
              AND e.endDate >= :from
            ORDER BY e.startDate ASC,
                     CASE WHEN e.startTime IS NULL THEN 1 ELSE 0 END ASC,
                     e.startTime ASC,
                     e.createdAt ASC
            """)
    List<Event> findOverlappingEvents(
            @Param("userId") Long userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    // BE-06: 로그인한 사용자 소유의 Event만 조회
    Optional<Event> findByIdAndUserId(Long id, Long userId);

    // BE-11: 오늘 날짜와 겹치는 일정 개수 조회
    @Query("""
            SELECT COUNT(e)
            FROM Event e
            WHERE e.user.id = :userId
              AND e.startDate <= :today
              AND e.endDate >= :today
            """)
    long countOverlappingEventsOnDate(
            @Param("userId") Long userId,
            @Param("today") LocalDate today
    );

    // BE-11: 오늘 이후 5일 이내 종료되는 기간 일정 조회
    @Query("""
            SELECT e
            FROM Event e
            WHERE e.user.id = :userId
              AND e.startDate < e.endDate
              AND e.endDate > :today
              AND e.endDate <= :limitDate
            ORDER BY e.endDate ASC,
                     e.createdAt ASC,
                     e.id ASC
            """)
    List<Event> findUpcomingPeriodEvents(
            @Param("userId") Long userId,
            @Param("today") LocalDate today,
            @Param("limitDate") LocalDate limitDate
    );
}
