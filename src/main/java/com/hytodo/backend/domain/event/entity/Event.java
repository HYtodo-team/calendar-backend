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
        validateTitle(title);
        validateSchedule(startDate, endDate, isAllDay, startTime, endTime);

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

    public void changeTitle(String title) {
        validateTitle(title);
        this.title = title;
    }

    /**
     * 날짜·시간·종일 여부를 함께 바꿉니다. 이 값들은 서로 의존하기 때문에
     * Service에서 정규화(기간 일정은 종일·시간 NULL)까지 끝낸 최종값을 받습니다.
     */
    public void changeSchedule(
            LocalDate startDate,
            LocalDate endDate,
            boolean isAllDay,
            LocalTime startTime,
            LocalTime endTime
    ) {
        validateSchedule(startDate, endDate, isAllDay, startTime, endTime);
        this.startDate = startDate;
        this.endDate = endDate;
        this.isAllDay = isAllDay;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void changeLocation(String location) {
        this.location = location;
    }

    public void changeMemo(String memo) {
        this.memo = memo;
    }

    public void changeImportant(boolean isImportant) {
        this.isImportant = isImportant;
    }

    private static void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
    }

    private static void validateSchedule(
            LocalDate startDate,
            LocalDate endDate,
            boolean isAllDay,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must not be after endDate");
        }

        if (!isAllDay) {
            if (startTime == null || endTime == null) {
                throw new IllegalArgumentException("startTime and endTime are required when not all day");
            }
            if (!startTime.isBefore(endTime)) {
                throw new IllegalArgumentException("startTime must be before endTime");
            }
        }
    }
}