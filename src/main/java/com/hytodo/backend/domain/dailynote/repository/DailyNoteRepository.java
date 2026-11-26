package com.hytodo.backend.domain.dailynote.repository;

import java.time.LocalDate;
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
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO daily_notes (user_id, note_date, content)
            VALUES (:userId, :noteDate, :content)
            ON DUPLICATE KEY UPDATE
                content = :content,
                updated_at = CURRENT_TIMESTAMP(6)
            """, nativeQuery = true)
    void upsert(
            @Param("userId") Long userId,
            @Param("noteDate") LocalDate noteDate,
            @Param("content") String content
    );
}
