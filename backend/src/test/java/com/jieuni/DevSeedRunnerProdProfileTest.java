package com.jieuni;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("prod")
class DevSeedRunnerProdProfileTest {

	@Autowired
	ApplicationContext context;

	@Test
	void prodProfileHasNoDevSeedRunner() {
		assertThat(context.containsBean("devSeedRunner")).isFalse();
	}

}
