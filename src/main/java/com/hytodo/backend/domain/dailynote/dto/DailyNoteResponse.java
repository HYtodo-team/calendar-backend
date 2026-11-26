package com.hytodo.backend.domain.dailynote.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hytodo.backend.domain.dailynote.entity.DailyNote;

public record DailyNoteResponse(
        Long id,
        LocalDate noteDate,
        String content,
        LocalDateTime updatedAt
) {

    public static DailyNoteResponse from(DailyNote note) {
        return new DailyNoteResponse(
                note.getId(),
                note.getNoteDate(),
                note.getContent(),
                note.getUpdatedAt()
        );
    }

    public static DailyNoteResponse empty(LocalDate noteDate) {
        return new DailyNoteResponse(null, noteDate, null, null);
    }
}
