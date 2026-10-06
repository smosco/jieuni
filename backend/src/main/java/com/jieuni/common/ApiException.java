package com.jieuni.common;

import org.springframework.http.HttpStatus;

import java.util.Map;

// 서비스가 상태 코드와 사람이 읽을 문장을 정해 던진다. 공개 시 검증 실패만 fieldErrors를 함께 담는다.
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final Map<String, String> fieldErrors;

	public ApiException(HttpStatus status, String message) {
		this(status, message, null);
	}

	public ApiException(HttpStatus status, String message, Map<String, String> fieldErrors) {
		super(message);
		this.status = status;
		this.fieldErrors = fieldErrors;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public Map<String, String> getFieldErrors() {
		return fieldErrors;
	}

}
