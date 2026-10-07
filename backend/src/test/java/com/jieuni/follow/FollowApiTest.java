package com.jieuni.follow;

import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FollowApiTest extends IntegrationTest {

	@Autowired
	EntityManager em;

	private void follow(Cookie session, long makerId) {
		assertThat(mvc.put().uri("/api/me/follows/" + makerId).cookie(session).with(xsrf()))
				.hasStatus(HttpStatus.NO_CONTENT);
	}

	private long place(String slug, PlaceStatus status, Instant publishedAt, long makerId) {
		long place = data.place(slug, status, publishedAt);
		data.credit(place, makerId, CreditRole.ARCHITECTURE, 0);
		return place;
	}

	private void later(Duration duration) {
		clock.set(clock.instant().plus(duration));
	}

	@Test
	void followIsIdempotentAndUnfollowRemoves() {
		Cookie me = devLogin("me@test.jieuni");
		long maker = data.maker("maker-a");

		follow(me, maker);
		follow(me, maker);
		assertThat(mvc.get().uri("/api/me/following/makers").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().containsExactly("maker-a");

		assertThat(mvc.delete().uri("/api/me/follows/" + maker).cookie(me).with(xsrf()))
				.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.delete().uri("/api/me/follows/" + maker).cookie(me).with(xsrf()))
				.hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/me/following/makers").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items").asArray().isEmpty();
	}

	@Test
	void unknownMakerIs404() {
		Cookie me = devLogin("me@test.jieuni");

		assertThat(mvc.put().uri("/api/me/follows/999999").cookie(me).with(xsrf()))
				.hasStatus(HttpStatus.NOT_FOUND).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"만든 사람을 찾을 수 없어요\"}");
	}

	@Test
	void followingPlacesAreDedupedWithFirstFollowedMakerAsCaption() {
		Cookie me = devLogin("me@test.jieuni");
		long a = data.maker("maker-a");
		long b = data.maker("maker-b");
		long notFollowed = data.maker("maker-c");
		long shared = data.place("shared", PlaceStatus.PUBLISHED, DEFAULT_NOW.minus(Duration.ofDays(1)));
		data.credit(shared, notFollowed, CreditRole.ARCHITECTURE, 0);
		data.credit(shared, b, CreditRole.LIGHTING, 1);
		data.credit(shared, a, CreditRole.LANDSCAPE, 2);
		place("only-a", PlaceStatus.PUBLISHED, DEFAULT_NOW.minus(Duration.ofDays(2)), a);
		place("only-c", PlaceStatus.PUBLISHED, DEFAULT_NOW.minus(Duration.ofDays(3)), notFollowed);
		place("closed-a", PlaceStatus.CLOSED, DEFAULT_NOW.minus(Duration.ofDays(4)), a);
		follow(me, a);
		follow(me, b);

		var json = assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson();
		json.extractingPath("$.items[*].slug").asArray().containsExactly("shared", "only-a");
		json.extractingPath("$.items[0].caption").isEqualTo("maker-b 이름");
		json.extractingPath("$.items[1].caption").isEqualTo("maker-a 이름");
	}

	@Test
	void isNewAndTabDotFollowSeenAt() {
		Cookie me = devLogin("me@test.jieuni");
		long a = data.maker("maker-a");
		place("old", PlaceStatus.PUBLISHED, DEFAULT_NOW.minus(Duration.ofDays(1)), a);
		follow(me, a); // 팔로우하기 전에 공개된 공간은 "새로운"이 아니다

		assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items[0].isNew").isEqualTo(false);
		assertThat(mvc.get().uri("/api/me").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.hasNewFromFollowing").isEqualTo(false);

		later(Duration.ofHours(1));
		place("new", PlaceStatus.PUBLISHED, clock.instant(), a);

		var before = assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson();
		before.extractingPath("$.items[*].slug").asArray().containsExactly("new", "old");
		before.extractingPath("$.items[*].isNew").asArray().containsExactly(true, false);
		assertThat(mvc.get().uri("/api/me").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.hasNewFromFollowing").isEqualTo(true);

		assertThat(mvc.post().uri("/api/me/following/seen").cookie(me).with(xsrf()))
				.hasStatus(HttpStatus.NO_CONTENT);

		assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].isNew").asArray().containsExactly(false, false);
		assertThat(mvc.get().uri("/api/me").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.hasNewFromFollowing").isEqualTo(false);
	}

	private void assertTabDot(Cookie session, boolean expected) {
		assertThat(mvc.get().uri("/api/me").cookie(session)).hasStatusOk().bodyJson()
				.extractingPath("$.hasNewFromFollowing").isEqualTo(expected);
	}

	// A를 팔로우하고 탭을 본 뒤 며칠 지나 B를 팔로우한 경우.
	@Test
	void lateFollowDoesNotMarkPastPlacesAsNew() {
		Cookie me = devLogin("me@test.jieuni");
		long a = data.maker("maker-a");
		long b = data.maker("maker-b");
		follow(me, a);
		assertThat(mvc.post().uri("/api/me/following/seen").cookie(me).with(xsrf())).hasStatus(HttpStatus.NO_CONTENT);

		later(Duration.ofDays(1));
		place("b-between", PlaceStatus.PUBLISHED, clock.instant(), b); // 탭을 본 뒤, B를 팔로우하기 전
		later(Duration.ofDays(2));
		follow(me, b);

		assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].isNew").asArray().containsExactly(false);
		assertTabDot(me, false);

		later(Duration.ofHours(1));
		place("b-new", PlaceStatus.PUBLISHED, clock.instant(), b);

		var json = assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson();
		json.extractingPath("$.items[*].slug").asArray().containsExactly("b-new", "b-between");
		json.extractingPath("$.items[*].isNew").asArray().containsExactly(true, false);
		assertTabDot(me, true);
	}

	// 같은 공간에 일찍 팔로우한 A와 늦게 팔로우한 B가 함께 있으면 A 기준으로 새롭다.
	@Test
	void placeIsNewIfNewForAnyFollowedMaker() {
		Cookie me = devLogin("me@test.jieuni");
		long a = data.maker("maker-a");
		long b = data.maker("maker-b");
		follow(me, a);
		later(Duration.ofDays(1));
		long shared = data.place("shared", PlaceStatus.PUBLISHED, clock.instant());
		data.credit(shared, b, CreditRole.ARCHITECTURE, 0);
		data.credit(shared, a, CreditRole.LIGHTING, 1);
		later(Duration.ofDays(1));
		follow(me, b);

		var json = assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson();
		json.extractingPath("$.items[0].isNew").isEqualTo(true);
		json.extractingPath("$.items[0].caption").isEqualTo("maker-b 이름");
		assertTabDot(me, true);
	}

	@Test
	void followingMakersAreRecentFirstWithLatestPhoto() {
		Cookie me = devLogin("me@test.jieuni");
		long a = data.maker("maker-a");
		long b = data.maker("maker-b");
		long withPhoto = place("a-place", PlaceStatus.PUBLISHED, DEFAULT_NOW.minus(Duration.ofDays(1)), a);
		data.photo(withPhoto, 0);
		follow(me, a);
		later(Duration.ofMinutes(1));
		follow(me, b);

		var json = assertThat(mvc.get().uri("/api/me/following/makers").cookie(me)).hasStatusOk().bodyJson();
		json.extractingPath("$.items[*].slug").asArray().containsExactly("maker-b", "maker-a");
		json.extractingPath("$.items[0].photo").isNull();
		json.extractingPath("$.items[1].photo.url.w800")
				.isEqualTo("https://photos.test/places/" + withPhoto + "/p0-800.webp");
	}

	@Test
	void otherUsersFollowsAreInvisible() {
		Cookie me = devLogin("me@test.jieuni");
		Cookie other = devLogin("other@test.jieuni");
		long a = data.maker("maker-a");
		place("a-place", PlaceStatus.PUBLISHED, DEFAULT_NOW.minus(Duration.ofDays(1)), a);
		follow(other, a);

		assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items").asArray().isEmpty();
		assertThat(mvc.get().uri("/api/me/following/makers").cookie(me)).hasStatusOk().bodyJson()
				.extractingPath("$.items").asArray().isEmpty();
	}

	@Test
	void listQueryCounts() {
		Cookie me = devLogin("me@test.jieuni");
		for (int i = 0; i < 3; i++) {
			long maker = data.maker("maker-" + i);
			long place = place("p" + i, PlaceStatus.PUBLISHED, DEFAULT_NOW.minus(Duration.ofDays(i + 1)), maker);
			data.photo(place, 0);
			follow(me, maker);
		}
		// 앞서 쌓인 INSERT가 세어지지 않게 내보내고, 실제 요청처럼 빈 영속성 컨텍스트에서 잰다.
		em.flush();
		em.clear();

		statistics().clear();
		assertThat(mvc.get().uri("/api/me/following/places").cookie(me)).hasStatusOk();
		// 사용자(본 시각) 1 · 공간+대표 사진 1 · 팔로우한 크레딧 1
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(3);

		em.clear();
		statistics().clear();
		assertThat(mvc.get().uri("/api/me/following/makers").cookie(me)).hasStatusOk();
		// 만든 사람 1 · 최근 사진 1
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);

		em.clear();
		statistics().clear();
		assertThat(mvc.get().uri("/api/me").cookie(me)).hasStatusOk();
		// 사용자 1 · 새 공간 있는지 1
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void anonymousIs401() {
		long a = data.maker("maker-a");

		assertThat(mvc.put().uri("/api/me/follows/" + a).with(xsrf())).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/me/following/places")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.post().uri("/api/me/following/seen").with(xsrf())).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
