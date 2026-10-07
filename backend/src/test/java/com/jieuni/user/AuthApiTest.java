package com.jieuni.user;

import com.jieuni.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni"; // application-test.yml의 ADMIN_EMAILS

	@Autowired
	UserService userService;

	private static List<String> setCookies(MvcTestResult result) {
		return result.getResponse().getHeaders("Set-Cookie");
	}

	private long userId(String email) {
		return data.jdbc().sql("select id from users where provider = 'dev' and provider_id = :email")
				.param("email", email).query(Long.class).single();
	}

	@Test
	void anonymousMeIs401WithoutCreatingSessionButGetsCsrfCookie() {
		MvcTestResult result = mvc.get().uri("/api/me").exchange();

		assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"로그인이 필요해요\"}");
		assertThat(setCookies(result)).noneMatch(c -> c.startsWith("SESSION="));
		assertThat(setCookies(result)).anyMatch(c -> c.startsWith("XSRF-TOKEN="));
	}

	@Test
	void postWithoutCsrfTokenIs403() {
		assertThat(mvc.post().uri("/api/dev/login")
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@test.jieuni\"}"))
				.hasStatus(HttpStatus.FORBIDDEN).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"권한이 없어요\"}");
	}

	// 서버가 GET 응답에 준 XSRF-TOKEN을 그대로 돌려보내는 실제 흐름.
	@Test
	void csrfCookieFromGetIsAcceptedAsHeader() {
		Cookie xsrf = mvc.get().uri("/api/me").exchange().getResponse().getCookie("XSRF-TOKEN");
		assertThat(xsrf).isNotNull();

		assertThat(mvc.post().uri("/api/dev/login").cookie(xsrf).header("X-XSRF-TOKEN", xsrf.getValue())
				.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@test.jieuni\"}"))
				.hasStatusOk();
	}

	@Test
	void devLoginCreatesUserAndSession() {
		// 쿠키 속성(HttpOnly·SameSite·Max-Age)은 MockMvc 응답에서 빠져 보여 실제 서버에 curl로 확인한다.
		Cookie session = devLogin("first.visitor@test.jieuni");

		assertThat(mvc.get().uri("/api/me").cookie(session)).hasStatusOk().bodyJson()
				.isStrictlyEqualTo("""
						{"id":%d,"nickname":"first.visitor","profileImageUrl":null,"role":"user","hasNewFromFollowing":false}"""
						.formatted(userId("first.visitor@test.jieuni")));
	}

	@Test
	void devLoginAgainUsesSameUser() {
		devLogin("a@test.jieuni");
		devLogin("a@test.jieuni");

		assertThat(data.jdbc().sql("select count(*) from users where provider_id = 'a@test.jieuni'")
				.query(Long.class).single()).isEqualTo(1);
	}

	@Test
	void loginChangesSessionId() {
		Cookie before = devLogin("a@test.jieuni");
		Cookie after = devLogin("b@test.jieuni", before);

		assertThat(after.getValue()).isNotEqualTo(before.getValue());
		assertThat(mvc.get().uri("/api/me").cookie(before)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/me").cookie(after)).hasStatusOk().bodyJson()
				.extractingPath("$.nickname").isEqualTo("b");
	}

	@Test
	void adminEmailBecomesAdmin() {
		Cookie session = devLogin(ADMIN);

		assertThat(mvc.get().uri("/api/me").cookie(session)).hasStatusOk().bodyJson()
				.extractingPath("$.role").isEqualTo("admin");
		// 관리자 인가를 통과하면 없는 경로라 404다.
		assertThat(mvc.get().uri("/api/admin/nothing").cookie(session)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void adminApiIs403ForUserAnd401ForAnonymous() {
		Cookie session = devLogin("a@test.jieuni");

		assertThat(mvc.get().uri("/api/admin/nothing").cookie(session))
				.hasStatus(HttpStatus.FORBIDDEN).bodyJson().isStrictlyEqualTo("{\"message\":\"권한이 없어요\"}");
		assertThat(mvc.get().uri("/api/admin/nothing"))
				.hasStatus(HttpStatus.UNAUTHORIZED).bodyJson().isStrictlyEqualTo("{\"message\":\"로그인이 필요해요\"}");
	}

	// ADMIN_EMAILS에서 빠진 관리자(DB에는 아직 admin)가 재기동 뒤 같은 세션으로 부르는 경우.
	@Test
	void adminRemovedFromAdminEmailsIsDeniedOnNextRequestWithoutRelogin() {
		Cookie session = devLogin("former.admin@test.jieuni");
		long id = userId("former.admin@test.jieuni");
		data.jdbc().sql("update users set role = 'admin' where id = :id").param("id", id).update();
		assertThat(mvc.get().uri("/api/admin/nothing").cookie(session)).hasStatus(HttpStatus.NOT_FOUND);

		userService.syncAdminRoles(); // 기동 때 하는 일

		assertThat(mvc.get().uri("/api/admin/nothing").cookie(session)).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void syncAdminRolesPromotesListedEmails() {
		devLogin("a@test.jieuni");
		data.jdbc().sql("update users set email = :admin where provider_id = 'a@test.jieuni'").param("admin", ADMIN).update();

		userService.syncAdminRoles();

		assertThat(data.jdbc().sql("select role from users where provider_id = 'a@test.jieuni'")
				.query(String.class).single()).isEqualTo("admin");
	}

	@Test
	void nicknameOver20Is400() {
		Cookie session = devLogin("a@test.jieuni");

		assertThat(mvc.patch().uri("/api/me").cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + "가".repeat(21) + "\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors.nickname").isEqualTo("닉네임은 20자까지 쓸 수 있어요");
	}

	@Test
	void nicknameChanges() {
		Cookie session = devLogin("a@test.jieuni");

		assertThat(mvc.patch().uri("/api/me").cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\" 지은 \"}"))
				.hasStatusOk().bodyJson().extractingPath("$.nickname").isEqualTo("지은");
	}

	@Test
	void logoutEndsSession() {
		Cookie session = devLogin("a@test.jieuni");

		MvcTestResult result = mvc.post().uri("/api/auth/logout").cookie(session).with(xsrf()).exchange();

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(setCookies(result)).anyMatch(c -> c.startsWith("SESSION=;"));
		assertThat(mvc.get().uri("/api/me").cookie(session)).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void anonymousLogoutIs204() {
		assertThat(mvc.post().uri("/api/auth/logout").with(xsrf())).hasStatus(HttpStatus.NO_CONTENT);
	}

}
