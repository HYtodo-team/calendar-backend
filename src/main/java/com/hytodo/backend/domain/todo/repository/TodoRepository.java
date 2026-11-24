package com.hytodo.backend.domain.todo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hytodo.backend.domain.todo.entity.Todo;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 전체 Todo 목록 조회 (미완료 우선, 최신순)
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId " +
           "AND (:completed IS NULL OR t.isCompleted = :completed) " +
           "ORDER BY t.isCompleted ASC, t.createdAt DESC")
    List<Todo> findAllByUserIdAndCompletedFilter(
            @Param("userId") Long userId,
            @Param("completed") Boolean completed
    );

    // 특정 사용자 소유의 Todo 조회
    Optional<Todo> findByIdAndUserId(Long id, Long userId);

    // 미완료 Todo 개수 조회
    long countByUserIdAndIsCompletedFalse(Long userId);

    // BE-06: 특정 Event에 연결된 Todo 목록 조회
    @Query("""
            SELECT t
            FROM Todo t
            WHERE t.user.id = :userId
              AND t.event.id = :eventId
            ORDER BY t.isCompleted ASC, t.createdAt DESC
            """)
    List<Todo> findAllByUserIdAndEventId(
            @Param("userId") Long userId,
            @Param("eventId") Long eventId
    );
}