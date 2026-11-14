package com.hytodo.backend.domain.event.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * 일정 부분 수정 요청입니다. 전달되지 않은 필드는 변경하지 않습니다.
 * {@code location}, {@code memo}는 빈 문자열을 보내면 값을 비웁니다.
 * 날짜·시간·종일 여부를 하나라도 보내면 서버가 최종 조합을 다시 정규화합니다.
 */
public record EventUpdateRequest(

        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        LocalDate startDate,

        LocalDate endDate,

        Boolean isAllDay,

        LocalTime startTime,

        LocalTime endTime,

        @Size(max = 255, message = "장소는 255자 이하여야 합니다.")
        String location,

        @Size(max = 10000, message = "메모는 10000자 이하여야 합니다.")
        String memo,

        Boolean isImportant
) {

    @AssertTrue(message = "수정할 값을 하나 이상 포함해야 합니다.")
    public boolean isAnyFieldPresent() {
        return title != null
                || startDate != null
                || endDate != null
                || isAllDay != null
                || startTime != null
                || endTime != null
                || location != null
                || memo != null
                || isImportant != null;
    }

    @AssertTrue(message = "제목은 공백일 수 없습니다.")
    public boolean isTitlePresentThenNotBlank() {
        return title == null || !title.isBlank();
    }
}
