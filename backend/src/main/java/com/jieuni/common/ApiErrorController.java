package com.jieuni.common;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 필터 단계 오류(방화벽 거부 400, 세션 저장소 장애 500 등)는 MVC 밖이라 /error로 온다. 본문을 { message }로 맞춘다.
@RestController
class ApiErrorController implements ErrorController {

	@RequestMapping("${server.error.path:/error}")
	ResponseEntity<ApiError> error(HttpServletRequest request) {
		HttpStatusCode status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) instanceof Integer code
				? HttpStatusCode.valueOf(code)
				: HttpStatus.NOT_FOUND; // /error를 직접 부른 경우
		return ResponseEntity.status(status).body(ApiError.of(status));
	}

}
