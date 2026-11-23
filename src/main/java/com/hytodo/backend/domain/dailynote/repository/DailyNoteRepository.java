package com.hytodo.backend.domain.dailynote.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hytodo.backend.domain.dailynote.entity.DailyNote;

public interface DailyNoteRepository extends JpaRepository<DailyNote, Long> {

    Optional<DailyNote> findByUserIdAndNoteDate(Long userId, LocalDate noteDate);
}
