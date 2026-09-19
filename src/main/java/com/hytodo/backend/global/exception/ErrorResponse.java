package com.hytodo.backend.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		boolean success,
		String errorCode,
		String message,
		LocalDateTime timestamp,
 		List<FieldError> fieldErrors
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), LocalDateTime.now(), null);
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(false, errorCode.getCode(), message, LocalDateTime.now(), null);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fieldErrors) {
		return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), LocalDateTime.now(), fieldErrors);
	}

	public record FieldError(String field, String message) {
	}
}

