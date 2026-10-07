package com.jieuni.common;

import com.jieuni.user.AdminAuthorizationManager;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository contextRepository,
			AdminAuthorizationManager adminAuthorization,
			@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {
		http
				.csrf(csrf -> csrf.spa())
				.addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
				.securityContext(context -> context.securityContextRepository(contextRepository))
				// 비로그인 요청을 세션에 저장하지 않는다(세션을 만들지 않게). 로그인 뒤 돌아갈 곳은 redirect 파라미터로 따로 받는다.
				.requestCache(cache -> cache.requestCache(new NullRequestCache()))
				// 401·403 본문은 ApiExceptionHandler가 쓴다.
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, e) -> exceptionResolver.resolveException(request, response, null, e))
						.accessDeniedHandler((request, response, e) -> exceptionResolver.resolveException(request, response, null, e)))
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)))
				.authorizeHttpRequests(auth -> auth
						// 오류 포워드는 원래 요청에서 이미 인가를 거쳤다. 막으면 원래 상태 대신 401·403이 보인다.
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						.requestMatchers("/actuator/health").permitAll()
						// 이 경로 아래 GET은 전부 공개다. 로그인이 필요한 GET은 다른 경로에 둔다.
						.requestMatchers(HttpMethod.GET, "/api/feed", "/api/places", "/api/places/**", "/api/makers",
								"/api/makers/**", "/api/sitemap").permitAll()
						// 운영 프로필에는 컨트롤러가 없어 404로 끝난다.
						.requestMatchers(HttpMethod.POST, "/api/dev/login").permitAll()
						.requestMatchers("/api/admin/**").access(adminAuthorization)
						.anyRequest().authenticated());
		return http.build();
	}

	// 개발용 로그인(수동 저장)과 OAuth 로그인이 같은 저장소를 쓴다. 구성은 Security 기본값과 같다.
	@Bean
	SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

}
