package com.hytodo.backend.global.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
		boolean success,
		String errorCode,
		String message,
		LocalDateTime timestamp
) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(false, errorCode.getCode(), errorCode.getMessage(), LocalDateTime.now());
	}

	public static ErrorResponse of(ErrorCode errorCode, String message) {
		return new ErrorResponse(false, errorCode.getCode(), message, LocalDateTime.now());
	}
}

