package com.hytodo.backend.domain.dailynote.entity;

import java.time.LocalDate;

import com.hytodo.backend.domain.user.entity.User;
import com.hytodo.backend.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "daily_notes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "note_date"})
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyNote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "note_date", nullable = false)
    private LocalDate noteDate;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder
    public DailyNote(User user, LocalDate noteDate, String content) {
        validateContent(content);

        this.user = user;
        this.noteDate = noteDate;
        this.content = content;
    }

    public void changeContent(String content) {
        validateContent(content);
        this.content = content;
    }

    private static void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("content must not be blank");
        }
    }
}
