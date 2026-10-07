package com.jieuni.user;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class NoStoreAuthorizedClientRepositoryTest {

	@Test
	void savedClientCannotBeLoaded() {
		var repository = new NoStoreAuthorizedClientRepository();
		var registration = ClientRegistration.withRegistrationId("google").clientId("c")
				.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
				.redirectUri("http://localhost:3000/login/oauth2/code/google")
				.authorizationUri("https://a.example").tokenUri("https://t.example").build();
		var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "secret-token", Instant.now(),
				Instant.now().plusSeconds(60));
		var principal = new TestingAuthenticationToken("1", null);
		var request = new MockHttpServletRequest();

		repository.saveAuthorizedClient(new OAuth2AuthorizedClient(registration, "1", token), principal, request,
				new MockHttpServletResponse());

		assertThat(repository.<OAuth2AuthorizedClient>loadAuthorizedClient("google", principal, request)).isNull();
		assertThat(request.getSession(false)).isNull();
	}

}
