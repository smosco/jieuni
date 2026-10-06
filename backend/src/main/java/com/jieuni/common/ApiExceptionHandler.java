package com.jieuni.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
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

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> handleApi(ApiException e) {
		return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getMessage(), e.getFieldErrors()));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> handleUnexpected(Exception e) {
		log.error("처리하지 못한 예외", e);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError("잠시 후 다시 시도해 주세요"));
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
		String message = statusCode.value() == 404 ? "찾을 수 없어요"
				: statusCode.is5xxServerError() ? "잠시 후 다시 시도해 주세요"
				: "요청을 처리할 수 없어요";
		return ResponseEntity.status(statusCode).headers(headers).body(new ApiError(message));
	}

}
