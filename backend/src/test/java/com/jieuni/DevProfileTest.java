package com.jieuni;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// ProdProfileTest가 빈 이름으로 "없다"를 확인하므로, 여기서 같은 이름으로 "있다"를 확인한다.
// dev 컨텍스트가 뜨면 시드가 실제로 적재된다. 시드 파일이 스키마 제약을 어기면 여기서 실패한다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "FIXED_NOW=2026-10-07T22:59:00Z")
@ActiveProfiles("dev")
class DevProfileTest {

	@Autowired
	ApplicationContext context;

	@Autowired
	Clock clock;

	@Autowired
	JdbcClient jdbc;

	@Test
	void hasDevSeedRunner() {
		assertThat(context.containsBean("devSeedRunner")).isTrue();
	}

	@Test
	void hasDevLogin() {
		assertThat(context.containsBean("devLoginController")).isTrue();
	}

	// 시드가 V3의 크레딧 CHECK(확인됨이면 출처 필수) 같은 제약을 어기면 컨텍스트가 뜨지 않는다.
	// 여기서는 확인된 크레딧이 실제로 들어갔는지까지 본다.
	@Test
	void seedLoadsVerifiedCreditsWithSources() {
		int verified = jdbc.sql("select count(*) from place_credits where verified").query(Integer.class).single();
		int verifiedWithoutSource = jdbc.sql("select count(*) from place_credits where verified and source_url is null")
				.query(Integer.class).single();
		assertThat(verified).isPositive();
		assertThat(verifiedWithoutSource).isZero();
	}

	@Test
	void fixedNowFixesClock() {
		assertThat(context.containsBean("devClock")).isTrue();
		assertThat(clock.instant()).isEqualTo(Instant.parse("2026-10-07T22:59:00Z"));
	}

}
