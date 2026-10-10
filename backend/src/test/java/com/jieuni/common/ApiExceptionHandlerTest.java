package com.jieuni.common;

import com.jieuni.IntegrationTest;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Import(ApiExceptionHandlerTest.ErrorController.class)
@WithMockUser
@ExtendWith(OutputCaptureExtension.class)
class ApiExceptionHandlerTest extends IntegrationTest {

	@Test
	void apiExceptionKeepsStatusAndMessageWithoutFieldErrors() {
		assertThat(mvc.get().uri("/test/errors/conflict"))
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"이미 있어요\"}");
	}

	@Test
	void apiExceptionWithFieldErrorsKeepsDottedKeys() {
		assertThat(mvc.get().uri("/test/errors/publish"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.extractingPath("$.fieldErrors['photos[1].sourceUrl']").isEqualTo("출처 링크를 입력해 주세요");
	}

	@Test
	void invalidBodyGivesFieldErrors() {
		assertThat(mvc.post().uri("/test/errors/valid").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"입력한 값을 확인해 주세요\",\"fieldErrors\":{\"nickname\":\"닉네임을 입력해 주세요\"}}");
	}

	@Test
	void unreadableJsonIs400WithMessage() {
		assertThat(mvc.post().uri("/test/errors/valid").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"요청을 처리할 수 없어요\"}");
	}

	@Test
	void unknownPathIs404WithMessage() {
		assertThat(mvc.get().uri("/test/errors/nothing-here"))
				.hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"찾을 수 없어요\"}");
	}

	@Test
	void wrongMethodIs405WithMessage() {
		assertThat(mvc.delete().uri("/test/errors/conflict").with(xsrf()))
				.hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"요청을 처리할 수 없어요\"}");
	}

	@Test
	void unexpectedExceptionIs500WithoutDetails() {
		assertThat(mvc.get().uri("/test/errors/boom"))
				.hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"잠시 후 다시 시도해 주세요\"}");
	}

	@Test
	void accessDeniedInControllerIs403Not500() {
		assertThat(mvc.get().uri("/test/errors/denied"))
				.hasStatus(HttpStatus.FORBIDDEN)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"권한이 없어요\"}");
	}

	@Test
	void authenticationExceptionInControllerIs401Not500() {
		assertThat(mvc.get().uri("/test/errors/unauthenticated"))
				.hasStatus(HttpStatus.UNAUTHORIZED)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"로그인이 필요해요\"}");
	}

	@Test
	void knownConstraintViolationIs409() {
		assertThat(mvc.get().uri("/test/errors/constraint/uq_places_slug"))
				.hasStatus(HttpStatus.CONFLICT)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"이미 다른 공간이 쓰는 주소(slug)예요\"}");
	}

	// 모르는 제약은 서버 버그라 500이고, 원인을 찾을 수 있게 원래 예외(제약 이름 포함)가 로그에 남아야 한다.
	@Test
	void unknownConstraintViolationIs500AndLogsOriginalException(CapturedOutput output) {
		assertThat(mvc.get().uri("/test/errors/constraint/ck_unknown_rule"))
				.hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"잠시 후 다시 시도해 주세요\"}");
		assertThat(output).contains("처리하지 못한 예외")
				.contains("DataIntegrityViolationException")
				.contains("ck_unknown_rule");
	}

	// 필터 단계 오류(방화벽 거부 등)가 컨테이너의 /error 포워드로 오는 경우. MockMvc는 포워드를 하지 않으므로 직접 꾸민다.
	@Test
	@WithAnonymousUser
	void errorDispatchKeepsStatusForAnonymous() {
		assertThat(mvc.get().uri("/error").with(request -> {
			request.setDispatcherType(DispatcherType.ERROR);
			request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 400);
			return request;
		}))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"요청을 처리할 수 없어요\"}");
	}

	@RestController
	static class ErrorController {

		record NicknameBody(@NotBlank(message = "닉네임을 입력해 주세요") @Size(max = 20) String nickname) {
		}

		@GetMapping("/test/errors/conflict")
		void conflict() {
			throw new ApiException(HttpStatus.CONFLICT, "이미 있어요");
		}

		@GetMapping("/test/errors/publish")
		void publish() {
			throw new ApiException(HttpStatus.BAD_REQUEST, "공개할 수 없어요",
					Map.of("photos[1].sourceUrl", "출처 링크를 입력해 주세요"));
		}

		@PostMapping("/test/errors/valid")
		void valid(@Valid @RequestBody NicknameBody body) {
		}

		@GetMapping("/test/errors/denied")
		void denied() {
			throw new AccessDeniedException("내부 정보");
		}

		@GetMapping("/test/errors/unauthenticated")
		void unauthenticated() {
			throw new AuthenticationCredentialsNotFoundException("내부 정보");
		}

		@GetMapping("/test/errors/constraint/{name}")
		void constraint(@PathVariable String name) {
			throw new DataIntegrityViolationException("위반",
					new ConstraintViolationException("제약 " + name + " 위반", new SQLException(name), name));
		}

		@GetMapping("/test/errors/boom")
		void boom() {
			throw new IllegalStateException("내부 정보가 담긴 메시지");
		}

	}

}
