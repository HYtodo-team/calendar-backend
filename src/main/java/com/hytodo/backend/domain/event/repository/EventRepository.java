package com.hytodo.backend.domain.event.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hytodo.backend.domain.event.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {

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
}