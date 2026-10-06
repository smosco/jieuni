package com.jieuni.common;

import com.jieuni.IntegrationTest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@Import(ApiExceptionHandlerTest.ErrorController.class)
@WithMockUser
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
		assertThat(mvc.post().uri("/test/errors/valid").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson()
				.isStrictlyEqualTo("{\"message\":\"입력한 값을 확인해 주세요\",\"fieldErrors\":{\"nickname\":\"닉네임을 입력해 주세요\"}}");
	}

	@Test
	void unreadableJsonIs400WithMessage() {
		assertThat(mvc.post().uri("/test/errors/valid").with(csrf())
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
		assertThat(mvc.delete().uri("/test/errors/conflict").with(csrf()))
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

		@GetMapping("/test/errors/boom")
		void boom() {
			throw new IllegalStateException("내부 정보가 담긴 메시지");
		}

	}

}
