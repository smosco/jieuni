package com.jieuni.common;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	// 사용자가 고칠 수 있는 DB 제약 위반 → 409 문구. 중복 여부는 미리 조회하지 않고 제약 하나로 판단한다(동시 요청도 같은 길).
	private static final Map<String, String> CONFLICTS = Map.of(
			"uq_places_slug", "이미 다른 공간이 쓰는 주소(slug)예요");

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApi(ApiException e) {
		return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getMessage(), e.getFieldErrors()));
	}

	// 아래 Exception 처리기가 인증·권한 예외를 500으로 바꾸지 않게 따로 받는다. Security의 401·403 응답도 여기로 위임된다.
	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ApiError> handleAuthentication(AuthenticationException e) {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(HttpStatus.UNAUTHORIZED));
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException e) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(HttpStatus.FORBIDDEN));
	}

	// 목록에 없는 제약 위반은 서버가 막았어야 할 값이므로 500으로 남긴다.
	@ExceptionHandler(DataIntegrityViolationException.class)
	ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e) {
		String name = constraintName(e);
		String message = name == null ? null : CONFLICTS.get(name);
		if (message == null) {
			return handleUnexpected(e);
		}
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(message));
	}

	private static String constraintName(Throwable e) {
		for (Throwable cause = e; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException violation) {
				return violation.getConstraintName();
			}
		}
		return null;
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception e) {
		log.error("처리하지 못한 예외", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(HttpStatus.INTERNAL_SERVER_ERROR));
	}

	// @Valid 실패: 필드마다 첫 메시지 하나.
	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getBindingResult().getFieldErrors()
				.forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.status(status).headers(headers).body(new ApiError("입력한 값을 확인해 주세요", fieldErrors));
	}

	// 나머지 Spring MVC 예외(없는 경로 404, 405, 읽을 수 없는 JSON 400 등)는 상태 코드를 그대로 두고 본문만 바꾼다.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		return ResponseEntity.status(statusCode).headers(headers).body(ApiError.of(statusCode));
	}

}
