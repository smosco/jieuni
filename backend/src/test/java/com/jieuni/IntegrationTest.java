package com.jieuni;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

// 통합 테스트 기반: 실제 PostgreSQL(Testcontainers) + MockMvc + 테스트마다 롤백.
// 같은 설정을 쓰는 테스트는 Spring 컨텍스트(와 컨테이너)를 함께 쓴다. 데이터는 각 테스트가 직접 만든다.
@Import({TestcontainersConfiguration.class, IntegrationTest.TestBeans.class})
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

	@Autowired
	protected TestData data;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@BeforeEach
	void resetClock() {
		clock.set(DEFAULT_NOW);
	}

	// 목록 API 쿼리 수 고정용(test 프로필에서 generate_statistics=true). 요청 직전에 clear()한다.
	protected Statistics statistics() {
		return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestBeans {

		@Bean
		@Primary
		MutableClock testClock() {
			return new MutableClock(DEFAULT_NOW);
		}

		@Bean
		TestData testData(JdbcClient jdbc) {
			return new TestData(jdbc);
		}

	}

}
