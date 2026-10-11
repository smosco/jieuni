package com.jieuni;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// 개발용 장치(시드, FIXED_NOW)는 운영 프로필에 존재하지 않는다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = { "FIXED_NOW=2000-01-01T00:00:00Z", "GOOGLE_CLIENT_ID=x", "GOOGLE_CLIENT_SECRET=x",
		"FRONTEND_URL=https://example.test", "S3_ENDPOINT=https://prod.r2.cloudflarestorage.com", "S3_BUCKET=b",
		"S3_ACCESS_KEY=x", "S3_SECRET_KEY=x", "REVALIDATE_SECRET=x" })
@ActiveProfiles("prod")
class ProdProfileTest {

	@Autowired
	ApplicationContext context;

	@Autowired
	Clock clock;

	@Test
	void noDevSeedRunner() {
		assertThat(context.containsBean("devSeedRunner")).isFalse();
	}

	@Test
	void noDevLogin() {
		assertThat(context.containsBean("devLoginController")).isFalse();
	}

	@Test
	void fixedNowIsIgnored() {
		assertThat(context.containsBean("devClock")).isFalse();
		assertThat(clock.instant()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
	}

	// 재생성 주소를 따로 주지 않으면 공개 주소로 부른다.
	@Test
	void revalidateUrlFallsBackToFrontendUrl() {
		assertThat(context.getEnvironment().getProperty("jieuni.revalidate.url")).isEqualTo("https://example.test");
	}

	// Caddy가 주는 X-Forwarded-Proto를 믿어야 XSRF-TOKEN 쿠키에도 Secure가 붙는다.
	@Test
	void trustsForwardedHeadersFromProxy() {
		assertThat(context.getEnvironment().getProperty("server.forward-headers-strategy")).isEqualTo("native");
	}

	// 비어 있으면 구글 로그인만 조용히 깨지므로 시작에서 멈춘다.
	@Test
	void prodRequiresGoogleClient() {
		assertThat(BackendApplication.missingProdEnv(prodEnv().withProperty("GOOGLE_CLIENT_SECRET", "")))
				.containsExactly("GOOGLE_CLIENT_SECRET");
	}

	// 시작 검사는 main()의 초기화기라 @SpringBootTest로는 돌지 않는다. 검사 함수를 직접 부른다.
	@Test
	void prodRequiresRevalidateSecret() {
		MockEnvironment env = prodEnv().withProperty("REVALIDATE_SECRET", "");
		assertThat(BackendApplication.missingProdEnv(env)).containsExactly("REVALIDATE_SECRET");
	}

	@Test
	void prodRequiresSomeRevalidateUrl() {
		assertThat(BackendApplication.missingProdEnv(prodEnv())).isEmpty();
		assertThat(BackendApplication.missingProdEnv(prodEnv().withProperty("FRONTEND_URL", "")
				.withProperty("REVALIDATE_URL", "http://frontend:3000"))).isEmpty();
		assertThat(BackendApplication.missingProdEnv(prodEnv().withProperty("FRONTEND_URL", "")))
				.containsExactly("REVALIDATE_URL 또는 FRONTEND_URL");
	}

	private static MockEnvironment prodEnv() {
		MockEnvironment env = new MockEnvironment();
		for (String name : new String[] { "DB_URL", "POSTGRES_USER", "POSTGRES_PASSWORD", "PHOTO_PUBLIC_BASE_URL",
				"S3_ENDPOINT", "S3_BUCKET", "S3_ACCESS_KEY", "S3_SECRET_KEY", "REVALIDATE_SECRET", "FRONTEND_URL",
				"GOOGLE_CLIENT_ID", "GOOGLE_CLIENT_SECRET" }) {
			env.setProperty(name, "x");
		}
		return env;
	}

}
