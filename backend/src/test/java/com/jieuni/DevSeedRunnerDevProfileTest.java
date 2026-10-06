package com.jieuni;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

// dev 컨텍스트가 뜨면 시드가 실제로 적재된다. 시드 파일이 스키마 제약을 어기면 여기서 실패한다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("dev")
class DevSeedRunnerDevProfileTest {

	@Autowired
	ApplicationContext context;

	@Test
	void devProfileHasDevSeedRunner() {
		assertThat(context.containsBean("devSeedRunner")).isTrue();
	}

}
