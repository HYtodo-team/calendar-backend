package com.hytodo.backend.domain.event.entity;

import java.time.LocalDate;
import java.time.LocalTime;

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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "events")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "is_all_day", nullable = false)
    private boolean isAllDay;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "memo")
    private String memo;

    @Column(name = "is_important", nullable = false)
    private boolean isImportant;

    @Builder
    public Event(
            User user,
            String title,
            LocalDate startDate,
            LocalDate endDate,
            LocalTime startTime,
            LocalTime endTime,
            boolean isAllDay,
            String location,
            String memo,
            boolean isImportant
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }

        this.user = user;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAllDay = isAllDay;
        this.location = location;
        this.memo = memo;
        this.isImportant = isImportant;
    }
}