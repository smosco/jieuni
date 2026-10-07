package com.jieuni.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatusCode;

import java.util.Map;

// 모든 오류 응답의 본문. fieldErrors가 없으면 키 자체를 빼서 { "message" }만 나간다.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, Map<String, String> fieldErrors) {

	public ApiError(String message) {
		this(message, null);
	}

	// 상태 코드만 있는 오류(Spring MVC 예외, 인증·권한, /error)의 문구를 한 곳에서 정한다.
	public static ApiError of(HttpStatusCode status) {
		return new ApiError(switch (status.value()) {
			case 401 -> "로그인이 필요해요";
			case 403 -> "권한이 없어요";
			case 404 -> "찾을 수 없어요";
			default -> status.is5xxServerError() ? "잠시 후 다시 시도해 주세요" : "요청을 처리할 수 없어요";
		});
	}

}
