package com.jieuni;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

// 개발용 장치(시드, FIXED_NOW)는 운영 프로필에 존재하지 않는다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "FIXED_NOW=2000-01-01T00:00:00Z")
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
	void fixedNowIsIgnored() {
		assertThat(context.containsBean("devClock")).isFalse();
		assertThat(clock.instant()).isAfter(Instant.parse("2026-01-01T00:00:00Z"));
	}

}
