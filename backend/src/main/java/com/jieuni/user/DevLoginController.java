package com.jieuni.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// 구글 없이 로그인 흐름을 확인하는 개발용 로그인. 운영 프로필에는 빈이 없다.
@Profile({"dev", "test"})
@RestController
class DevLoginController {

	private final UserService userService;

	private final SecurityContextRepository contextRepository;

	DevLoginController(UserService userService, SecurityContextRepository contextRepository) {
		this.userService = userService;
		this.contextRepository = contextRepository;
	}

	record DevLoginRequest(@NotBlank @Email String email) {
	}

	@PostMapping("/api/dev/login")
	MeResponse login(@Valid @RequestBody DevLoginRequest body, HttpServletRequest request,
			HttpServletResponse response) {
		String email = body.email();
		LoginUser user = userService.login("dev", email, email, email.substring(0, email.indexOf('@')), null);

		// 로그인 전 세션 ID를 그대로 쓰면 세션 고정 공격에 열린다.
		if (request.getSession(false) != null) {
			request.changeSessionId();
		}
		// 세션에만 저장한다. 이 요청의 나머지는 인증이 필요 없고, 스레드의 컨텍스트를 바꾸면 테스트(같은 스레드)로 새어 나간다.
		SecurityContext context = new SecurityContextImpl(
				UsernamePasswordAuthenticationToken.authenticated(user, null, List.of()));
		contextRepository.saveContext(context, request, response);
		return userService.me(user.id());
	}

}
