package com.jieuni.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

// 모든 오류 응답의 본문(API.md 0절). fieldErrors가 없으면 키 자체를 빼서 { "message" }만 나간다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, Map<String, String> fieldErrors) {

	public ApiError(String message) {
		this(message, null);
	}

}
