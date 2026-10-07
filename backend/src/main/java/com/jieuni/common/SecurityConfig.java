package com.jieuni.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(auth -> auth
				.requestMatchers("/actuator/health").permitAll()
				// 이 경로 아래 GET은 전부 공개다. 로그인이 필요한 GET은 다른 경로에 둔다.
				.requestMatchers(HttpMethod.GET, "/api/feed", "/api/places", "/api/places/**", "/api/makers",
						"/api/makers/**", "/api/sitemap").permitAll()
				.anyRequest().authenticated());
		return http.build();
	}

}
