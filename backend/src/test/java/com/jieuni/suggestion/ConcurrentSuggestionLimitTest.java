package com.jieuni.suggestion;

import com.jieuni.IntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

// 한도 직전에 요청이 몰려도 하루 10건을 넘지 않는다.
// 요청마다 실제로 커밋돼야 하므로 테스트 트랜잭션을 쓰지 않고, 만든 데이터는 직접 지운다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrentSuggestionLimitTest extends IntegrationTest {

	private static final String EMAIL = "concurrent-suggest@test.jieuni";

	@AfterEach
	void cleanUp() {
		var jdbc = data.jdbc();
		jdbc.sql("delete from places where suggested_by in (select id from users where provider_id = :email)")
				.param("email", EMAIL).update();
		jdbc.sql("delete from users where provider_id = :email").param("email", EMAIL).update();
	}

	// 한도 직전(9건)에서 여러 요청이 동시에 오면 한 건만 통과해야 한다.
	@Test
	void concurrentSuggestionsAtTheLimitLetOnlyOneThrough() throws Exception {
		Cookie me = devLogin(EMAIL);
		long userId = data.jdbc().sql("select id from users where provider_id = :email").param("email", EMAIL)
				.query(Long.class).single();
		for (int i = 0; i < SuggestionService.DAILY_LIMIT - 1; i++) {
			data.jdbc().sql("""
					insert into places (name, region, status, suggested_by, created_at, updated_at)
					values (:name, '서울', 'suggested', :userId, :at, :at)""")
					.param("name", "이미 보낸 제보 " + i).param("userId", userId)
					.param("at", clock.instant().atOffset(java.time.ZoneOffset.UTC)).update();
		}
		int requests = 20;
		ExecutorService pool = Executors.newFixedThreadPool(requests);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();
		for (int i = 0; i < requests; i++) {
			String name = "동시 제보 " + i;
			results.add(pool.submit(() -> {
				start.await();
				return mvc.post().uri("/api/suggestions/places").cookie(me).with(xsrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"" + name + "\",\"region\":\"서울\",\"reason\":\"이유\"}")
						.exchange().getResponse().getStatus();
			}));
		}
		start.countDown();
		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> result : results) {
			statuses.add(result.get());
		}
		pool.shutdown();

		assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
		assertThat(statuses).filteredOn(s -> s == 429).hasSize(requests - 1);
	}

}
