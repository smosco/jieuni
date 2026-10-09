package com.jieuni.save;

import com.jieuni.IntegrationTest;
import com.jieuni.place.PlaceStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

// 같은 요청이 동시에 와도(더블 탭, 재시도) 전부 성공하고 행은 하나다.
// 요청마다 실제로 커밋돼야 하므로 테스트 트랜잭션을 쓰지 않고, 만든 데이터는 직접 지운다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrentSaveFollowTest extends IntegrationTest {

	private static final int REQUESTS = 8;

	@AfterEach
	void cleanUp() {
		var jdbc = data.jdbc();
		jdbc.sql("delete from saves where user_id in (select id from users where provider_id = 'concurrent@test.jieuni')").update();
		jdbc.sql("delete from follows where user_id in (select id from users where provider_id = 'concurrent@test.jieuni')").update();
		jdbc.sql("delete from users where provider_id = 'concurrent@test.jieuni'").update();
		jdbc.sql("delete from place_credits where place_id in (select id from places where slug = 'concurrent-place')").update();
		jdbc.sql("delete from places where slug = 'concurrent-place'").update();
		jdbc.sql("delete from makers where slug = 'concurrent-maker'").update();
	}

	private List<Integer> sendAtOnce(Supplier<MockMvcRequestBuilder> request) throws Exception {
		ExecutorService pool = Executors.newFixedThreadPool(REQUESTS);
		CountDownLatch start = new CountDownLatch(1);
		List<Future<Integer>> results = new ArrayList<>();
		for (int i = 0; i < REQUESTS; i++) {
			results.add(pool.submit(() -> {
				start.await();
				return request.get().exchange().getResponse().getStatus();
			}));
		}
		start.countDown();
		List<Integer> statuses = new ArrayList<>();
		for (Future<Integer> result : results) {
			statuses.add(result.get());
		}
		pool.shutdown();
		return statuses;
	}

	@Test
	void concurrentSavesAllSucceedWithOneRow() throws Exception {
		Cookie me = devLogin("concurrent@test.jieuni");
		long place = data.place("concurrent-place", PlaceStatus.PUBLISHED, Instant.parse("2026-09-01T00:00:00Z"));

		List<Integer> statuses = sendAtOnce(() -> mvc.put().uri("/api/me/saves/" + place).cookie(me).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"want\"}"));

		assertThat(statuses).containsOnly(200);
		assertThat(data.jdbc().sql("select count(*) from saves where place_id = :id").param("id", place)
				.query(Long.class).single()).isEqualTo(1);
	}

	@Test
	void concurrentFollowsAllSucceedWithOneRow() throws Exception {
		Cookie me = devLogin("concurrent@test.jieuni");
		long maker = data.maker("concurrent-maker");

		List<Integer> statuses = sendAtOnce(() -> mvc.put().uri("/api/me/follows/" + maker).cookie(me).with(xsrf()));

		assertThat(statuses).containsOnly(204);
		assertThat(data.jdbc().sql("select count(*) from follows where maker_id = :id").param("id", maker)
				.query(Long.class).single()).isEqualTo(1);
	}

}
