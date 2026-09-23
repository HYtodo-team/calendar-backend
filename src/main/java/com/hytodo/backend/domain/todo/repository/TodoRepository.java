package com.hytodo.backend.domain.todo.repository;

import com.hytodo.backend.domain.todo.entity.Todo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 전체 투두 목록 (미완료 우선, 최신순 정렬)
    @Query("SELECT t FROM Todo t WHERE t.user.id = :userId " +
           "AND (:completed IS NULL OR t.isCompleted = :completed) " +
           "ORDER BY t.isCompleted ASC, t.createdAt DESC")
    List<Todo> findAllByUserIdAndCompletedFilter(@Param("userId") Long userId, @Param("completed") Boolean completed);

    // 단일 투두 소유권 조회
    Optional<Todo> findByIdAndUserId(Long id, Long userId);

    // 미완료 투두 개수 집계
    long countByUserIdAndIsCompletedFalse(Long userId);
}