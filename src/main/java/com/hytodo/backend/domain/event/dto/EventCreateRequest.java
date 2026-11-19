package com.hytodo.backend.domain.event.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EventCreateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotNull(message = "시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "종료일은 필수입니다.")
        LocalDate endDate,

        @NotNull(message = "종일 여부는 필수입니다.")
        Boolean isAllDay,

        LocalTime startTime,

        LocalTime endTime,

        @Size(max = 255, message = "장소는 255자 이하여야 합니다.")
        String location,

        @Size(max = 10000, message = "메모는 10000자 이하여야 합니다.")
        String memo,

        Boolean isImportant
) {
}
