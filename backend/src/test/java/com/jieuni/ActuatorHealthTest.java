package com.jieuni;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActuatorHealthTest extends IntegrationTest {

	@Test
	void healthIsOpenWithoutLogin() {
		assertThat(mvc.get().uri("/actuator/health"))
				.hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("UP");
	}

}
