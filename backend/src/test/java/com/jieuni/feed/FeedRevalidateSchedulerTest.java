package com.jieuni.feed;

import com.jieuni.common.FakeRevalidateClient;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.support.CronExpression;

import java.time.ZonedDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FeedRevalidateSchedulerTest {

	private static ZonedDateTime next(String cron, String kst) {
		return CronExpression.parse(cron).next(kst(kst));
	}

	private static ZonedDateTime kst(String local) {
		return ZonedDateTime.parse(local + "+09:00[Asia/Seoul]");
	}

	@Test
	void sendsFeed() {
		FakeRevalidateClient client = new FakeRevalidateClient();

		new FeedRevalidateScheduler(client).scheduled();

		assertThat(client.calls()).containsExactly(Set.of("feed"));
	}

	@Test
	void runsJustAfterEightOnWeekdaysOnly() {
		String cron = FeedRevalidateScheduler.OPENS;
		// 2026-10-07은 수요일.
		assertThat(next(cron, "2026-10-07T07:59:59")).isEqualTo(kst("2026-10-07T08:00:05"));
		assertThat(next(cron, "2026-10-07T08:00:05")).isEqualTo(kst("2026-10-08T08:00:05"));
		assertThat(next(cron, "2026-10-09T08:01:00")).isEqualTo(kst("2026-10-12T08:00:05"));
		assertThat(next(cron, "2026-10-10T12:00:00")).isEqualTo(kst("2026-10-12T08:00:05"));
	}

	// 주말 자정에도 돈다. 일요일 자정에 "다음 주"가 "내일"로 바뀐다.
	@Test
	void runsJustAfterMidnightEveryDay() {
		String cron = FeedRevalidateScheduler.MIDNIGHT;
		assertThat(next(cron, "2026-10-07T23:59:59")).isEqualTo(kst("2026-10-08T00:00:05"));
		assertThat(next(cron, "2026-10-10T08:00:00")).isEqualTo(kst("2026-10-11T00:00:05"));
	}

}
