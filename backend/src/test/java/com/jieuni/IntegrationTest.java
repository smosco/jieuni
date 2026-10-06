package com.jieuni;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// 통합 테스트 기반: 실제 PostgreSQL(Testcontainers) + MockMvc + 테스트마다 롤백.
// 같은 설정을 쓰는 테스트는 Spring 컨텍스트(와 컨테이너)를 함께 쓴다. 데이터는 각 테스트가 직접 만든다.
@Import({TestcontainersConfiguration.class, IntegrationTest.TestClockConfiguration.class})
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class IntegrationTest {

	// 2026-10-07(수) 10:00 KST
	protected static final Instant DEFAULT_NOW = Instant.parse("2026-10-07T01:00:00Z");

	@Autowired
	protected MockMvcTester mvc;

	@Autowired
	protected MutableClock clock;

	@BeforeEach
	void resetClock() {
		clock.set(DEFAULT_NOW);
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestClockConfiguration {

		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(DEFAULT_NOW);
		}

	}

}
