package com.jieuni.maker;

import com.jayway.jsonpath.JsonPath;
import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MakerPublicApiTest extends IntegrationTest {

	private static Instant daysAgo(int days) {
		return DEFAULT_NOW.minus(days, ChronoUnit.DAYS);
	}

	@Test
	void profileShowsPlacesWithPagesOnceWithFirstRoleAsCaption() {
		long studio = data.maker("studio", MakerKind.STUDIO);
		long maker = data.maker("seojin");
		data.jdbc().sql("update makers set affiliation_id = :studio, region = '서울 종로구' where id = :id")
				.param("studio", studio).param("id", maker).update();
		long newer = data.place("newer", PlaceStatus.PUBLISHED, daysAgo(1));
		data.credit(newer, maker, CreditRole.LIGHTING, 1);
		data.credit(newer, maker, CreditRole.LANDSCAPE, 0);
		data.credit(data.place("closed", PlaceStatus.CLOSED, daysAgo(5)), maker, CreditRole.INTERIOR, 0);
		data.credit(data.place("draft", PlaceStatus.DRAFT, null), maker, CreditRole.INTERIOR, 0);

		var json = assertThat(mvc.get().uri("/api/makers/seojin")).hasStatusOk().bodyJson();
		json.extractingPath("$.places[*].slug").asArray().containsExactly("newer", "closed");
		json.extractingPath("$.places[*].caption").asArray().containsExactly("조경", "인테리어");
		json.extractingPath("$.places[1].status").isEqualTo("closed");
		json.extractingPath("$.affiliation.slug").isEqualTo("studio");
		json.extractingPath("$.region").isEqualTo("서울 종로구");
		json.extractingPath("$.kind").isEqualTo("person");
	}

	@Test
	void makerWithoutPublicPlacesIs404() {
		long maker = data.maker("hidden");
		data.credit(data.place("draft", PlaceStatus.DRAFT, null), maker, CreditRole.INTERIOR, 0);
		data.credit(data.place("suggested", PlaceStatus.SUGGESTED, null), maker, CreditRole.INTERIOR, 0);

		assertThat(mvc.get().uri("/api/makers/hidden")).hasStatus(HttpStatus.NOT_FOUND)
				.bodyJson().extractingPath("$.message").isEqualTo("만든 사람을 찾을 수 없어요");
		assertThat(mvc.get().uri("/api/makers/nobody")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void profileRunsTwoQueries() {
		long maker = data.maker("seojin");
		for (int i = 0; i < 3; i++) {
			long p = data.place("p" + i, PlaceStatus.PUBLISHED, daysAgo(i));
			data.credit(p, maker, CreditRole.INTERIOR, 0);
			data.photo(p, 0);
		}

		statistics().clear();
		assertThat(mvc.get().uri("/api/makers/seojin")).hasStatusOk();
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void browseListsMakersWithListedPlacesByLatestPublish() {
		long old = data.maker("old");
		long recent = data.maker("recent");
		long closedOnly = data.maker("closed-only");
		long draftOnly = data.maker("draft-only");
		data.credit(data.place("old-place", PlaceStatus.PUBLISHED, daysAgo(10)), old, CreditRole.INTERIOR, 0);
		data.credit(data.place("recent-old", PlaceStatus.PUBLISHED, daysAgo(9)), recent, CreditRole.INTERIOR, 0);
		data.credit(data.place("recent-new", PlaceStatus.PAUSED, daysAgo(1)), recent, CreditRole.INTERIOR, 0);
		data.credit(data.place("closed", PlaceStatus.CLOSED, daysAgo(0)), closedOnly, CreditRole.INTERIOR, 0);
		data.credit(data.place("draft", PlaceStatus.DRAFT, null), draftOnly, CreditRole.INTERIOR, 0);

		assertThat(mvc.get().uri("/api/makers")).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().containsExactly("recent", "old");
	}

	@Test
	void browsePhotoIsFromTheLatestPlaceThatHasOne() {
		long maker = data.maker("seojin");
		long withPhoto = data.place("with-photo", PlaceStatus.PUBLISHED, daysAgo(3));
		data.photo(withPhoto, 0);
		data.credit(withPhoto, maker, CreditRole.INTERIOR, 0);
		data.credit(data.place("no-photo", PlaceStatus.PUBLISHED, daysAgo(1)), maker, CreditRole.INTERIOR, 0);

		assertThat(mvc.get().uri("/api/makers")).hasStatusOk().bodyJson()
				.extractingPath("$.items[0].photo.url.w800")
				.isEqualTo("https://photos.test/places/" + withPhoto + "/p0-800.webp");
	}

	@Test
	void browseCursorWalksToTheEndWithoutDuplicates() throws UnsupportedEncodingException {
		for (int i = 1; i <= 5; i++) {
			long m = data.maker("m" + i);
			// m2·m3은 최근 공개 시각이 같다.
			data.credit(data.place("p" + i, PlaceStatus.PUBLISHED, daysAgo(i == 3 ? 2 : i)), m, CreditRole.INTERIOR, 0);
		}

		List<String> seen = new ArrayList<>();
		String cursor = null;
		do {
			String body = mvc.get().uri("/api/makers?size=2" + (cursor == null ? "" : "&cursor=" + cursor))
					.exchange().getResponse().getContentAsString();
			seen.addAll(JsonPath.read(body, "$.items[*].slug"));
			cursor = JsonPath.read(body, "$.nextCursor");
		} while (cursor != null);

		assertThat(seen).containsExactly("m1", "m3", "m2", "m4", "m5");
	}

	@Test
	void browseRunsTwoQueries() {
		for (int i = 0; i < 4; i++) {
			long m = data.maker("m" + i);
			long p = data.place("p" + i, PlaceStatus.PUBLISHED, daysAgo(i));
			data.credit(p, m, CreditRole.INTERIOR, 0);
			data.photo(p, 0);
		}

		statistics().clear();
		assertThat(mvc.get().uri("/api/makers?size=3")).hasStatusOk();
		// 목록 1 · 그 만든 사람들의 대표 사진 1
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

}
