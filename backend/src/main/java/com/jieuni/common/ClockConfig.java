package com.jieuni.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Configuration
public class ClockConfig {

	private static final Logger log = LoggerFactory.getLogger(ClockConfig.class);

	@Bean
	@Profile("!dev")
	Clock clock() {
		return Clock.systemUTC();
	}

	// dev에서만 FIXED_NOW(ISO-8601, 예: 2026-10-07T22:59:00Z)로 시계를 고정한다. 피드 08:00 경계를 손으로 볼 때 쓴다.
	@Bean
	@Profile("dev")
	Clock devClock(@Value("${FIXED_NOW:}") String fixedNow) {
		Clock clock = fixedNow.isBlank() ? Clock.systemUTC() : Clock.fixed(Instant.parse(fixedNow), ZoneOffset.UTC);
		log.info("시계: {} (지금 {})", fixedNow.isBlank() ? "시스템 시각" : "FIXED_NOW로 고정", clock.instant());
		return clock;
	}

}
