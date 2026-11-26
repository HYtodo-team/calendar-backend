package com.hytodo.backend.domain.dailynote.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hytodo.backend.domain.dailynote.entity.DailyNote;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long> {

    Optional<DailyNote> findByUserIdAndNoteDate(Long userId, LocalDate noteDate);

    /**
     * (userId, noteDate) 기준 네이티브 upsert. UNIQUE(user_id, note_date) 제약 위반 없이
     * 동시 PUT 요청을 DB 레벨에서 원자적으로 처리한다.
     * created_at/updated_at은 DB 세션 시각(CURRENT_TIMESTAMP) 대신 애플리케이션이 전달한
     * now 값을 사용해, BaseTimeEntity의 JPA 감사(auditing) 타임스탬프와 시간 기준을 통일한다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO daily_notes (user_id, note_date, content, created_at, updated_at)
            VALUES (:userId, :noteDate, :content, :now, :now)
            ON DUPLICATE KEY UPDATE
                content = :content,
                updated_at = :now
            """, nativeQuery = true)
    void upsert(
            @Param("userId") Long userId,
            @Param("noteDate") LocalDate noteDate,
            @Param("content") String content,
            @Param("now") LocalDateTime now
    );
}