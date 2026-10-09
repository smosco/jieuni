package com.jieuni.save;

import com.jayway.jsonpath.JsonPath;
import com.jieuni.IntegrationTest;
import com.jieuni.place.PlaceStatus;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SaveApiTest extends IntegrationTest {

	@Autowired
	EntityManager em;

	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	private MvcTestResult put(Cookie session, long placeId, String status) {
		return mvc.put().uri("/api/me/saves/" + placeId).cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}").exchange();
	}

	private long saveRows(long placeId) {
		return data.jdbc().sql("select count(*) from saves where place_id = :id").param("id", placeId)
				.query(Long.class).single();
	}

	@Test
	void wantThenBeenUpdatesSameRow() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(put(me, place, "want")).hasStatusOk().bodyJson()
				.isStrictlyEqualTo("{\"placeId\":" + place + ",\"status\":\"want\"}");
		assertThat(put(me, place, "been")).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("been");

		assertThat(mvc.get().uri("/api/me/saves?status=want").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items").asArray().isEmpty();
		assertThat(mvc.get().uri("/api/me/saves?status=been").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().containsExactly("quiet-hill");
		assertThat(saveRows(place)).isEqualTo(1);
	}

	@Test
	void sameStatusAgainKeepsSavedTime() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);
		Instant first = clock.instant();
		put(me, place, "want");
		clock.set(first.plus(Duration.ofHours(1)));
		put(me, place, "want");

		assertThat(data.jdbc().sql("select updated_at from saves where place_id = :id").param("id", place)
				.query(Instant.class).single()).isEqualTo(first);

		put(me, place, "been");
		assertThat(data.jdbc().sql("select updated_at from saves where place_id = :id").param("id", place)
				.query(Instant.class).single()).isEqualTo(first.plus(Duration.ofHours(1)));
	}

	@Test
	void deleteRemovesAndIsIdempotent() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);
		put(me, place, "want");

		assertThat(mvc.delete().uri("/api/me/saves/" + place).cookie(me).with(xsrf()))
				.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.delete().uri("/api/me/saves/" + place).cookie(me).with(xsrf()))
				.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(saveRows(place)).isZero();
	}

	@Test
	void closedPlaceIs409AndUnpublishedIs404() {
		Cookie me = devLogin("me@test.jieuni");
		long closed = data.place("gone", PlaceStatus.CLOSED, PUBLISHED);
		long draft = data.place("draft", PlaceStatus.DRAFT, null);

		assertThat(put(me, closed, "want")).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"이제는 만날 수 없는 공간이라 저장할 수 없어요\"}");
		assertThat(put(me, draft, "want")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(put(me, 999_999, "want")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(put(me, draft, "maybe")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void closedPlaceSavedBeforeStillListed() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("soon-gone", PlaceStatus.PUBLISHED, PUBLISHED);
		put(me, place, "been");
		data.jdbc().sql("update places set status = 'closed' where id = :id").param("id", place).update();

		assertThat(mvc.get().uri("/api/me/saves?status=been").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items[0].status").isEqualTo("closed");
	}

	@Test
	void listIsRecentFirstWithCursorAndHidesOthers() {
		Cookie me = devLogin("me@test.jieuni");
		Cookie other = devLogin("other@test.jieuni");
		long a = data.place("a", PlaceStatus.PUBLISHED, PUBLISHED);
		long b = data.place("b", PlaceStatus.PUBLISHED, PUBLISHED);
		long c = data.place("c", PlaceStatus.PUBLISHED, PUBLISHED);
		data.photo(b, 0);
		put(me, a, "want");
		clock.set(clock.instant().plus(Duration.ofMinutes(1)));
		put(me, b, "want");
		clock.set(clock.instant().plus(Duration.ofMinutes(1)));
		put(me, c, "want");
		clock.set(clock.instant().plus(Duration.ofMinutes(1)));
		put(me, a, "been");
		clock.set(clock.instant().plus(Duration.ofMinutes(1)));
		put(me, a, "want"); // 상태가 바뀌면 그 목록의 맨 앞으로 온다
		put(other, b, "want");

		MvcTestResult first = mvc.get().uri("/api/me/saves?status=want&size=2").cookie(me).exchange();
		var firstJson = assertThat(first).hasStatusOk().bodyJson();
		firstJson.extractingPath("$.items[*].slug").asArray().containsExactly("a", "c");
		firstJson.extractingPath("$.items[0].caption").isNull();
		String cursor = JsonPath.read(content(first), "$.nextCursor");

		var second = assertThat(mvc.get().uri("/api/me/saves?status=want&size=2&cursor=" + cursor).cookie(me))
				.hasStatusOk().bodyJson();
		second.extractingPath("$.items[*].slug").asArray().containsExactly("b");
		second.extractingPath("$.items[0].photo.url.w800").isEqualTo("https://photos.test/places/" + b + "/p0-800.webp");
		second.extractingPath("$.nextCursor").isNull();

		assertThat(mvc.get().uri("/api/me/saves?status=want").cookie(other)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().containsExactly("b");
	}

	private static String content(MvcTestResult result) {
		try {
			return result.getResponse().getContentAsString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@Test
	void listRunsOneQuery() {
		Cookie me = devLogin("me@test.jieuni");
		for (int i = 0; i < 3; i++) {
			long place = data.place("p" + i, PlaceStatus.PUBLISHED, PUBLISHED);
			data.photo(place, 0);
			put(me, place, "want");
		}

		// 앞서 쌓인 INSERT가 세어지지 않게 내보내고, 실제 요청처럼 빈 영속성 컨텍스트에서 잰다.
		em.flush();
		em.clear();

		statistics().clear();
		assertThat(mvc.get().uri("/api/me/saves?status=want").cookie(me)).hasStatusOk();
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
	}

	@Test
	void statusParamIsRequiredAndChecked() {
		Cookie me = devLogin("me@test.jieuni");

		assertThat(mvc.get().uri("/api/me/saves").cookie(me)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/me/saves?status=maybe").cookie(me)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void anonymousIs401() {
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(mvc.get().uri("/api/me/saves?status=want")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.put().uri("/api/me/saves/" + place).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"want\"}"))
				.hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
