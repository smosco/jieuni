package com.jieuni.place;

import com.jayway.jsonpath.JsonPath;
import com.jieuni.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 로그인 없이 부른다(공개 API).
class PlacePublicApiTest extends IntegrationTest {

	private static Instant daysAgo(int days) {
		return DEFAULT_NOW.minus(days, ChronoUnit.DAYS);
	}

	@Test
	void detailHasChildrenInPositionOrderAndBuiltUrls() {
		long a = data.maker("maker-a");
		long b = data.maker("maker-b");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, daysAgo(1));
		data.jdbc().sql("""
				update places set address = '서울 종로구 1', lat = 37.5, lng = 127.0, access = 'ticket',
				  summary = '한 문장', impression = '감상', sense_light = '빛', visited_by_editor_on = '2026-05-17'
				where id = :id""").param("id", place).update();
		data.credit(place, b, CreditRole.LIGHTING, 1);
		data.credit(place, a, CreditRole.ARCHITECTURE, 0);
		data.photo(place, 1);
		data.photo(place, 0);
		data.jdbc().sql("insert into place_sources (place_id, title, url, type, position) values (:id, '기사', 'https://example.com/a', 'article', 0)")
				.param("id", place).update();

		var json = assertThat(mvc.get().uri("/api/places/quiet-hill")).hasStatusOk().bodyJson();
		json.extractingPath("$.credits[*].maker.slug").asArray().containsExactly("maker-a", "maker-b");
		json.extractingPath("$.credits[0].role").isEqualTo("architecture");
		json.extractingPath("$.kind").isEqualTo("garden");
		json.extractingPath("$.access").isEqualTo("ticket");
		json.extractingPath("$.senses.light").isEqualTo("빛");
		json.extractingPath("$.senses.sound").isNull();
		json.extractingPath("$.visitedByEditorOn").isEqualTo("2026-05");
		json.extractingPath("$.photos[0].url.w1600").isEqualTo("https://photos.test/places/" + place + "/p0-1600.webp");
		json.extractingPath("$.photos[0].url.w800").isEqualTo("https://photos.test/places/" + place + "/p0-800.webp");
		json.extractingPath("$.photos[0].license").isEqualTo("own");
		json.extractingPath("$.sources[0].type").isEqualTo("article");
		json.extractingPath("$.embeds").asArray().isEmpty();
		json.extractingPath("$.mapLinks.kakao").asString()
				.startsWith("https://map.kakao.com/link/map/quiet-hill%20%EC%9D%B4%EB%A6%84,37.5");
		json.extractingPath("$.mapLinks.naver").asString().startsWith("https://map.naver.com/p/search/");
	}

	@Test
	void kakaoLinkEncodesCommaInName() {
		long place = data.place("comma", PlaceStatus.PUBLISHED, daysAgo(1));
		data.jdbc().sql("update places set name = '카페, 정원', lat = 37.5, lng = 127.0 where id = :id")
				.param("id", place).update();

		assertThat(mvc.get().uri("/api/places/comma")).hasStatusOk().bodyJson()
				.extractingPath("$.mapLinks.kakao")
				.isEqualTo("https://map.kakao.com/link/map/%EC%B9%B4%ED%8E%98%2C%20%EC%A0%95%EC%9B%90,37.500000,127.000000");
	}

	@Test
	void relatedPlacesAreOtherListedPlacesOfTheSameMakersUpToSix() {
		long a = data.maker("maker-a");
		long other = data.maker("maker-other");
		long place = data.place("main", PlaceStatus.PUBLISHED, daysAgo(0));
		data.credit(place, a, CreditRole.ARCHITECTURE, 0);
		List<String> expected = new ArrayList<>();
		for (int i = 1; i <= 7; i++) {
			long p = data.place("by-a-" + i, i == 4 ? PlaceStatus.PAUSED : PlaceStatus.PUBLISHED, daysAgo(i));
			data.credit(p, a, CreditRole.INTERIOR, 0);
			data.credit(p, a, CreditRole.LIGHTING, 1); // 같은 사람의 크레딧이 둘이어도 한 번만
			if (i <= 6) {
				expected.add("by-a-" + i);
			}
		}
		data.credit(data.place("closed-by-a", PlaceStatus.CLOSED, daysAgo(1)), a, CreditRole.INTERIOR, 0);
		data.credit(data.place("draft-by-a", PlaceStatus.DRAFT, null), a, CreditRole.INTERIOR, 0);
		data.credit(data.place("by-other", PlaceStatus.PUBLISHED, daysAgo(1)), other, CreditRole.INTERIOR, 0);

		assertThat(mvc.get().uri("/api/places/main")).hasStatusOk().bodyJson()
				.extractingPath("$.relatedPlaces[*].slug").asArray().containsExactlyElementsOf(expected);
	}

	@Test
	void onlyPlacesWithAPageAreVisible() {
		data.place("draft", PlaceStatus.DRAFT, null);
		data.place("suggested", PlaceStatus.SUGGESTED, null);
		data.place("rejected", PlaceStatus.REJECTED, null);
		data.place("closed", PlaceStatus.CLOSED, daysAgo(3));

		for (String slug : List.of("draft", "suggested", "rejected", "nothing")) {
			assertThat(mvc.get().uri("/api/places/" + slug)).hasStatus(HttpStatus.NOT_FOUND)
					.bodyJson().extractingPath("$.message").isEqualTo("공간을 찾을 수 없어요");
		}
		assertThat(mvc.get().uri("/api/places/closed")).hasStatusOk()
				.bodyJson().extractingPath("$.status").isEqualTo("closed");
	}

	@Test
	void detailRunsFiveQueries() {
		long a = data.maker("maker-a");
		long place = data.place("main", PlaceStatus.PUBLISHED, daysAgo(0));
		data.credit(place, a, CreditRole.ARCHITECTURE, 0);
		data.photo(place, 0);
		data.credit(data.place("other", PlaceStatus.PUBLISHED, daysAgo(1)), a, CreditRole.ARCHITECTURE, 0);

		statistics().clear();
		assertThat(mvc.get().uri("/api/places/main")).hasStatusOk();
		// 공간+크레딧+만든 사람 1 · 출처 1 · 사진 1 · 임베드 1 · 관련 공간 1
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(5);
	}

	@Test
	void browseListsPublishedAndPausedNewestFirst() {
		data.place("older", PlaceStatus.PAUSED, daysAgo(2));
		data.place("newer", PlaceStatus.PUBLISHED, daysAgo(1));
		data.place("closed", PlaceStatus.CLOSED, daysAgo(0));
		data.place("draft", PlaceStatus.DRAFT, null);
		data.place("suggested", PlaceStatus.SUGGESTED, null);

		var json = assertThat(mvc.get().uri("/api/places")).hasStatusOk().bodyJson();
		json.extractingPath("$.items[*].slug").asArray().containsExactly("newer", "older");
		json.extractingPath("$.items[0].caption").isNull();
		json.extractingPath("$.nextCursor").isNull();
	}

	@Test
	void browseFiltersByManyKinds() {
		data.place("garden", PlaceStatus.PUBLISHED, daysAgo(1), PlaceKind.GARDEN);
		data.place("cafe", PlaceStatus.PUBLISHED, daysAgo(2), PlaceKind.CAFE);
		data.place("bar", PlaceStatus.PUBLISHED, daysAgo(3), PlaceKind.BAR);

		assertThat(mvc.get().uri("/api/places?kind=garden,cafe")).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().containsExactly("garden", "cafe");
	}

	@Test
	void badListParametersAre400() {
		assertThat(mvc.get().uri("/api/places?kind=garden,castle")).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.message").isEqualTo("알 수 없는 종류예요: castle");
		assertThat(mvc.get().uri("/api/places?cursor=not-a-cursor")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/places?size=61")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/places?size=0")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void cursorWalksToTheEndWithoutDuplicates() throws UnsupportedEncodingException {
		// 공개 시각이 같은 두 곳은 id로 순서가 갈린다.
		List<String> all = List.of("p5", "p4", "p3", "p2", "p1");
		data.place("p1", PlaceStatus.PUBLISHED, daysAgo(3));
		data.place("p2", PlaceStatus.PUBLISHED, daysAgo(2));
		data.place("p3", PlaceStatus.PUBLISHED, daysAgo(2));
		data.place("p4", PlaceStatus.PUBLISHED, daysAgo(1));
		data.place("p5", PlaceStatus.PUBLISHED, daysAgo(1));

		List<String> seen = new ArrayList<>();
		String cursor = null;
		int pages = 0;
		do {
			MvcTestResult result = mvc.get().uri("/api/places?size=2" + (cursor == null ? "" : "&cursor=" + cursor))
					.exchange();
			String body = result.getResponse().getContentAsString();
			seen.addAll(JsonPath.read(body, "$.items[*].slug"));
			cursor = JsonPath.read(body, "$.nextCursor");
			pages++;
		} while (cursor != null);

		assertThat(seen).containsExactlyElementsOf(all);
		assertThat(pages).isEqualTo(3);
	}

	@Test
	void browseRunsOneQuery() {
		for (int i = 0; i < 5; i++) {
			long p = data.place("p" + i, PlaceStatus.PUBLISHED, daysAgo(i));
			data.photo(p, 0);
		}

		statistics().clear();
		assertThat(mvc.get().uri("/api/places?size=3")).hasStatusOk().bodyJson()
				.extractingPath("$.items[0].photo.url.w800").isNotNull();
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(1);
	}

}
