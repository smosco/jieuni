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
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	// 사용자가 고칠 수 있는 DB 제약 위반 → 409 문구. 중복 여부는 미리 조회하지 않고 제약 하나로 판단한다(동시 요청도 같은 길).
	private static final Map<String, String> CONFLICTS = Map.of(
			"uq_places_slug", "이미 다른 공간이 쓰는 주소(slug)예요",
			"uq_makers_slug", "이미 다른 만든 사람이 쓰는 주소(slug)예요",
			"uq_place_photos_object_key", "이미 등록된 사진이에요",
			"uq_daily_picks_place", "이미 다른 날에 편성된 공간이에요",
			// 같은 빈 날에 두 편성이 동시에 들어오면 둘째가 기본키에 걸린다.
			"daily_picks_pkey", "방금 다른 편성이 저장됐어요");

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

	// 목록 본문(@Valid List<T>)은 메서드 검증으로 돈다. 키는 매개변수 이름 + 원소 번호 + 필드("credits[0].sourceUrl").
	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getParameterValidationResults().stream()
				.sorted(Comparator.comparing(r -> r.getContainerIndex() == null ? -1 : r.getContainerIndex()))
				.forEach(result -> {
					String key = result.getMethodParameter().getParameterName()
							+ (result.getContainerIndex() == null ? "" : "[" + result.getContainerIndex() + "]");
					if (result instanceof ParameterErrors errors) {
						errors.getFieldErrors().forEach(e -> fieldErrors.putIfAbsent(key + "." + e.getField(), e.getDefaultMessage()));
					} else {
						result.getResolvableErrors().forEach(e -> fieldErrors.putIfAbsent(key, e.getDefaultMessage()));
					}
				});
		return ResponseEntity.status(status).headers(headers).body(new ApiError("입력한 값을 확인해 주세요", fieldErrors));
	}

	// 나머지 Spring MVC 예외(없는 경로 404, 405, 읽을 수 없는 JSON 400 등)는 상태 코드를 그대로 두고 본문만 바꾼다.
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		return ResponseEntity.status(statusCode).headers(headers).body(ApiError.of(statusCode));
	}

}
