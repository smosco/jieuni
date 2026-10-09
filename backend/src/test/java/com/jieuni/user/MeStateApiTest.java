package com.jieuni.user;

import com.jieuni.IntegrationTest;
import com.jieuni.place.PlaceStatus;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class MeStateApiTest extends IntegrationTest {

	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	@Autowired
	EntityManager em;

	private void save(Cookie session, long placeId, String status) {
		assertThat(mvc.put().uri("/api/me/saves/" + placeId).cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}")).hasStatusOk();
	}

	@Test
	void returnsOnlyMyStateForAskedIds() {
		Cookie me = devLogin("me@test.jieuni");
		Cookie other = devLogin("other@test.jieuni");
		long p1 = data.place("p1", PlaceStatus.PUBLISHED, PUBLISHED);
		long p2 = data.place("p2", PlaceStatus.PUBLISHED, PUBLISHED);
		long p3 = data.place("p3", PlaceStatus.PUBLISHED, PUBLISHED);
		long m1 = data.maker("m1");
		long m2 = data.maker("m2");
		save(me, p1, "want");
		save(me, p2, "been");
		save(other, p3, "want");
		assertThat(mvc.put().uri("/api/me/follows/" + m1).cookie(me).with(xsrf())).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.put().uri("/api/me/follows/" + m2).cookie(other).with(xsrf())).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(mvc.get().uri("/api/me/state?places=%d,%d,%d&makers=%d,%d".formatted(p1, p2, p3, m1, m2)).cookie(me))
				.hasStatusOk().bodyJson()
				.isStrictlyEqualTo("{\"saves\":{\"%d\":\"want\",\"%d\":\"been\"},\"follows\":[%d]}".formatted(p1, p2, m1));
	}

	@Test
	void emptyParamsGiveEmptyStateWithoutQueries() {
		Cookie me = devLogin("me@test.jieuni");

		statistics().clear();
		assertThat(mvc.get().uri("/api/me/state").cookie(me)).hasStatusOk().bodyJson()
				.isStrictlyEqualTo("{\"saves\":{},\"follows\":[]}");
		assertThat(statistics().getPrepareStatementCount()).isZero();
	}

	@Test
	void runsTwoQueries() {
		Cookie me = devLogin("me@test.jieuni");
		long p1 = data.place("p1", PlaceStatus.PUBLISHED, PUBLISHED);
		save(me, p1, "want");
		// 앞서 쌓인 INSERT가 세어지지 않게 내보내고, 실제 요청처럼 빈 영속성 컨텍스트에서 잰다.
		em.flush();
		em.clear();

		statistics().clear();
		assertThat(mvc.get().uri("/api/me/state?places=" + p1 + "&makers=1,2").cookie(me)).hasStatusOk();
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void tooManyOrInvalidIdsAre400() {
		Cookie me = devLogin("me@test.jieuni");
		String hundred = IntStream.rangeClosed(1, 100).mapToObj(String::valueOf).collect(Collectors.joining(","));

		assertThat(mvc.get().uri("/api/me/state?places=" + hundred).cookie(me)).hasStatusOk();
		assertThat(mvc.get().uri("/api/me/state?makers=" + hundred + ",101").cookie(me))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"makers는 한 번에 100개까지 물을 수 있어요\"}");
		assertThat(mvc.get().uri("/api/me/state?makers=1,x").cookie(me)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void anonymousIs401() {
		assertThat(mvc.get().uri("/api/me/state?places=1")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
