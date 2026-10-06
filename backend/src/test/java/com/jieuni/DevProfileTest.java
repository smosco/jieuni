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

	@Test
	void hasDevSeedRunner() {
		assertThat(context.containsBean("devSeedRunner")).isTrue();
	}

	@Test
	void fixedNowFixesClock() {
		assertThat(context.containsBean("devClock")).isTrue();
		assertThat(clock.instant()).isEqualTo(Instant.parse("2026-10-07T22:59:00Z"));
	}

}
