package com.hytodo.backend.global.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();
		return ResponseEntity
				.status(errorCode.getStatus())
				.body(ErrorResponse.of(errorCode, exception.getMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ErrorResponse.FieldError fieldError =
				new ErrorResponse.FieldError(ex.getParameterName(), "필수 파라미터입니다.");
		return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, List.of(fieldError)));
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.collect(Collectors.toList());
		return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, fieldErrors));
	}

	// @Validated로 감싼 path/query 파라미터 검증 실패
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
		List<ErrorResponse.FieldError> fieldErrors = exception.getConstraintViolations().stream()
				.map(this::toFieldError)
				.collect(Collectors.toList());
		return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, fieldErrors));
	}

	// 깨진 JSON 본문
	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.badRequest()
				.body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "요청 본문을 읽을 수 없습니다. JSON 형식을 확인해 주세요."));
	}

	// 파라미터 타입 변환 실패 (예: eventId=abc)
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
		ErrorResponse.FieldError fieldError =
				new ErrorResponse.FieldError(exception.getName(), "값의 형식이 올바르지 않습니다.");
		return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, List.of(fieldError)));
	}


	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception exception) {
		log.error("예상하지 못한 예외가 발생했습니다.", exception);
		return ResponseEntity
				.internalServerError()
				.body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
	}

	private ErrorResponse.FieldError toFieldError(ConstraintViolation<?> violation) {
		String path = violation.getPropertyPath().toString();
		String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
		return new ErrorResponse.FieldError(field, violation.getMessage());
	}
}

