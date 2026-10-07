package com.jieuni.user;

import com.jieuni.IntegrationTest;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// 구글과의 실제 왕복은 테스트하지 않는다. 우리 코드(리졸버·성공/실패 처리)와 Security가 그 코드를 부르는 경로만 본다.
class OAuthLoginTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni"; // application-test.yml의 ADMIN_EMAILS

	@Autowired
	ReturnPathAuthorizationRequestResolver resolver;

	@Autowired
	OAuthLoginHandler handler;

	@Autowired
	EntityManager em;

	@Autowired
	FilterChainProxy securityFilters;

	private static OAuth2AuthenticationToken google(String sub, String email, Boolean verified, String name,
			String picture) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("sub", sub);
		claims.put("email", email);
		claims.put("email_verified", verified);
		claims.put("name", name);
		claims.put("picture", picture);
		claims.values().removeIf(v -> v == null);
		OidcIdToken idToken = new OidcIdToken("id-token", DEFAULT_NOW, DEFAULT_NOW.plusSeconds(3600), claims);
		return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), idToken), List.of(), "google");
	}

	private static MockHttpServletRequest callback(String returnPath) {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
		if (returnPath != null) {
			MockHttpSession session = new MockHttpSession();
			session.setAttribute("jieuni.returnPath", returnPath);
			request.setSession(session);
		}
		return request;
	}

	private MockHttpServletResponse succeed(MockHttpServletRequest request, OAuth2AuthenticationToken token)
			throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();
		handler.onAuthenticationSuccess(request, response, token);
		return response;
	}

	private Map<String, Object> userRow(String sub) {
		return data.jdbc().sql("select * from users where provider = 'google' and provider_id = :sub")
				.param("sub", sub).query().singleRow();
	}

	@Test
	void authorizationRedirectUsesFrontendCallbackBehindProxy() {
		// Next rewrites는 Host를 백엔드 주소로 바꿔 보낸다. 그래도 redirect_uri는 프론트 주소여야 한다.
		var result = mvc.get().uri("/oauth2/authorization/google?redirect=/places/x")
				.header("Host", "backend:8080").header("X-Forwarded-Host", "localhost:3000").exchange();

		assertThat(result).hasStatus(HttpStatus.FOUND);
		assertThat(result.getResponse().getRedirectedUrl())
				.startsWith("https://accounts.google.com/o/oauth2/v2/auth?")
				.contains("redirect_uri=http://localhost:3000/login/oauth2/code/google")
				.contains("scope=openid%20email%20profile");
	}

	// 로그인 필터는 받은 토큰을 이 저장소 하나에만 저장한다. 기본값(메모리 맵)이 아니어야 한다.
	@Test
	void loginFilterDoesNotKeepProviderTokens() {
		OAuth2LoginAuthenticationFilter filter = securityFilters.getFilterChains().getFirst().getFilters().stream()
				.filter(OAuth2LoginAuthenticationFilter.class::isInstance)
				.map(OAuth2LoginAuthenticationFilter.class::cast)
				.findFirst().orElseThrow();

		assertThat(ReflectionTestUtils.getField(filter, "authorizedClientRepository"))
				.isInstanceOf(NoStoreAuthorizedClientRepository.class);
	}

	@Test
	void resolverRemembersOnlySafeReturnPath() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
		request.setServletPath("/oauth2/authorization/google");
		request.setParameter("redirect", "/places/x");
		OAuth2AuthorizationRequest authorization = resolver.resolve(request);

		assertThat(authorization.getRedirectUri()).isEqualTo("http://localhost:3000/login/oauth2/code/google");
		assertThat(request.getSession().getAttribute("jieuni.returnPath")).isEqualTo("/places/x");

		MockHttpServletRequest evil = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
		evil.setServletPath("/oauth2/authorization/google");
		evil.setParameter("redirect", "/\t/evil.example");
		resolver.resolve(evil);
		assertThat(evil.getSession().getAttribute("jieuni.returnPath")).isEqualTo("/");

		MockHttpServletRequest korean = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
		korean.setServletPath("/oauth2/authorization/google");
		korean.setParameter("redirect", "/makers/이로재");
		resolver.resolve(korean);
		assertThat(korean.getSession().getAttribute("jieuni.returnPath")).isEqualTo("/makers/%EC%9D%B4%EB%A1%9C%EC%9E%AC");
	}

	@Test
	void successCreatesUserStoresLoginUserAndReturns() throws Exception {
		MockHttpServletRequest request = callback("/places/x");
		String longName = "가".repeat(19) + "🙂" + "나".repeat(5);

		MockHttpServletResponse response = succeed(request,
				google("g-1", "visitor@gmail.com", true, longName, "https://lh3.googleusercontent.com/a/1"));

		assertThat(response.getRedirectedUrl()).isEqualTo("/places/x");
		Map<String, Object> row = userRow("g-1");
		assertThat(row.get("nickname")).isEqualTo("가".repeat(19) + "🙂");
		assertThat(row.get("email")).isEqualTo("visitor@gmail.com");
		assertThat(row.get("role")).isEqualTo("user");
		SecurityContext context = (SecurityContext) request.getSession()
				.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
		assertThat(context.getAuthentication().getPrincipal()).isEqualTo(new LoginUser((Long) row.get("id")));
		assertThat(request.getSession().getAttribute("jieuni.returnPath")).isNull();
	}

	@Test
	void unverifiedEmailIsNotStoredNorComparedWithAdminEmails() throws Exception {
		succeed(callback(null), google("g-2", ADMIN, false, "관리자인 척", null));

		Map<String, Object> row = userRow("g-2");
		assertThat(row.get("email")).isNull();
		assertThat(row.get("role")).isEqualTo("user");
	}

	@Test
	void verifiedAdminEmailBecomesAdmin() throws Exception {
		succeed(callback(null), google("g-3", ADMIN, true, "관리자", null));

		assertThat(userRow("g-3").get("role")).isEqualTo("admin");
	}

	@Test
	void nicknameFallsBackToEmailThenDefault() throws Exception {
		succeed(callback(null), google("g-4", "no.name@gmail.com", true, null, null));
		succeed(callback(null), google("g-5", null, null, "  ", null));

		assertThat(userRow("g-4").get("nickname")).isEqualTo("no.name");
		assertThat(userRow("g-5").get("nickname")).isEqualTo("방문자");
	}

	@Test
	void secondLoginKeepsNicknameAndUpdatesPicture() throws Exception {
		succeed(callback(null), google("g-6", "a@gmail.com", true, "처음 이름", "https://lh3.googleusercontent.com/a/old"));
		// 실제로는 로그인마다 트랜잭션이 커밋된다. 테스트는 한 트랜잭션이라 영속성 컨텍스트를 직접 비운다.
		em.flush();
		em.clear();
		data.jdbc().sql("update users set nickname = '내가 바꾼 이름' where provider_id = 'g-6'").update();

		succeed(callback(null), google("g-6", "a@gmail.com", true, "구글 새 이름", "https://lh3.googleusercontent.com/a/new"));
		em.flush();

		assertThat(data.jdbc().sql("select count(*) from users where provider_id = 'g-6'").query(Long.class).single())
				.isEqualTo(1);
		Map<String, Object> row = userRow("g-6");
		assertThat(row.get("nickname")).isEqualTo("내가 바꾼 이름");
		assertThat(row.get("profile_image_url")).isEqualTo("https://lh3.googleusercontent.com/a/new");
	}

	@Test
	void failureReturnsWithLoginError() throws Exception {
		var denied = new OAuth2AuthenticationException(new OAuth2Error("access_denied"));

		MockHttpServletResponse withPath = new MockHttpServletResponse();
		handler.onAuthenticationFailure(callback("/places/x"), withPath, denied);
		MockHttpServletResponse withQuery = new MockHttpServletResponse();
		handler.onAuthenticationFailure(callback("/places?kind=garden"), withQuery, denied);
		MockHttpServletResponse withoutSession = new MockHttpServletResponse();
		handler.onAuthenticationFailure(callback(null), withoutSession, denied);

		assertThat(withPath.getRedirectedUrl()).isEqualTo("/places/x?login=error");
		assertThat(withQuery.getRedirectedUrl()).isEqualTo("/places?kind=garden&login=error");
		assertThat(withoutSession.getRedirectedUrl()).isEqualTo("/?login=error");
	}

	// 구글에서 취소하면 콜백에 error가 온다. Security 필터가 실패 처리기를 부르는지 실제 경로로 본다.
	@Test
	void cancelledCallbackGoesThroughFailureHandler() {
		var result = mvc.get().uri("/login/oauth2/code/google?error=access_denied&state=x").exchange();

		assertThat(result).hasStatus(HttpStatus.FOUND);
		assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/?login=error");
	}

}
