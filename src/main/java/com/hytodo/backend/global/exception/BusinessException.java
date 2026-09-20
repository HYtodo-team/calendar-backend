package com.hytodo.backend.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/* 로그인 실패 사유(가입 안 됨 vs 비밀번호 틀림)를 절대 구분해서 노출하면 안 되므로,
	 커스텀 메시지를 넣을 수 있는 생성자를 대신 이 factory 하나로만 던지게 강제한다.
	*/
	public static BusinessException invalidCredentials() {
		return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
	}

}

