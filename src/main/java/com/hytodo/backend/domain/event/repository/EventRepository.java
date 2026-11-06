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
}