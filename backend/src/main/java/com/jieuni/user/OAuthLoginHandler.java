package com.jieuni.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

// 로그인 성공: 제공자 사용자 → users upsert → 세션에는 LoginUser만 남긴다.
// 제공자 토큰은 세션에도 메모리에도 남지 않는다(인가된 클라이언트 저장소가 NoStoreAuthorizedClientRepository).
// 실패·취소: 돌아갈 경로에 login=error를 붙여 보낸다. Location은 상대 경로라 브라우저가 있는 프론트 주소로 간다.
@Component
public class OAuthLoginHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

	private static final Logger log = LoggerFactory.getLogger(OAuthLoginHandler.class);

	private final UserService userService;

	private final SecurityContextRepository contextRepository;

	OAuthLoginHandler(UserService userService, SecurityContextRepository contextRepository) {
		this.userService = userService;
		this.contextRepository = contextRepository;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
		LoginUser user = switch (token.getAuthorizedClientRegistrationId()) {
			case GoogleProfile.PROVIDER -> {
				GoogleProfile profile = GoogleProfile.from((OidcUser) token.getPrincipal());
				yield userService.login(GoogleProfile.PROVIDER, profile.providerId(), profile.email(),
						profile.nickname(), profile.profileImageUrl());
			}
			default -> throw new IllegalStateException("모르는 제공자: " + token.getAuthorizedClientRegistrationId());
		};
		contextRepository.saveContext(
				new SecurityContextImpl(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of())),
				request, response);
		response.sendRedirect(ReturnPath.take(request));
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException {
		log.info("소셜 로그인 실패: {}", exception.getMessage());
		String path = ReturnPath.take(request);
		response.sendRedirect(path + (path.contains("?") ? "&" : "?") + "login=error");
	}

}
