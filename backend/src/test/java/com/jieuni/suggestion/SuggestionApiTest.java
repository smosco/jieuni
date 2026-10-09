package com.jieuni.suggestion;

import com.jieuni.IntegrationTest;
import com.jieuni.place.PlaceStatus;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SuggestionApiTest extends IntegrationTest {

	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	@Autowired
	EntityManager em;

	private MvcTestResult suggestPlace(Cookie session, String name) {
		return mvc.post().uri("/api/suggestions/places").cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"%s","region":"서울 종로구","link":"https://example.com/a","reason":"빛이 오래 남는다","makerHint":"이로재"}"""
						.formatted(name))
				.exchange();
	}

	private MvcTestResult suggestCredit(Cookie session, long placeId, String body) {
		return mvc.post().uri("/api/suggestions/credits").cookie(session).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body.formatted(placeId)).exchange();
	}

	private MvcTestResult suggestCredit(Cookie session, long placeId) {
		return suggestCredit(session, placeId, """
				{"placeId":%d,"role":"lighting","makerName":"빛 설계자","sourceUrl":"https://example.com/c","isSelf":false}""");
	}

	private void nowKst(String localDateTime) {
		clock.set(java.time.LocalDateTime.parse(localDateTime).atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant());
	}

	@Test
	void placeSuggestionIsSavedAsSuggestedAndHiddenFromPublicApi() {
		Cookie me = devLogin("me@test.jieuni");

		MvcTestResult result = suggestPlace(me, "숨은 정원");

		assertThat(result).hasStatus(HttpStatus.CREATED).bodyJson().extractingPath("$.status").isEqualTo("suggested");
		var row = data.jdbc().sql("""
				select p.slug, p.status, p.suggestion_reason, p.suggestion_link, p.suggestion_maker_hint, u.provider_id
				from places p join users u on u.id = p.suggested_by where p.name = '숨은 정원'""").query().singleRow();
		assertThat(row.get("slug")).isNull();
		assertThat(row.get("status")).isEqualTo("suggested");
		assertThat(row.get("suggestion_reason")).isEqualTo("빛이 오래 남는다");
		assertThat(row.get("suggestion_link")).isEqualTo("https://example.com/a");
		assertThat(row.get("suggestion_maker_hint")).isEqualTo("이로재");
		assertThat(row.get("provider_id")).isEqualTo("me@test.jieuni");
		assertThat(mvc.get().uri("/api/places")).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].name").asArray().doesNotContain("숨은 정원");
	}

	@Test
	void sameNameAsPublicPlaceIs409WithSlug() {
		Cookie me = devLogin("me@test.jieuni");
		data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED); // 이름: "quiet-hill 이름"

		assertThat(suggestPlace(me, "  QUIET-HILL 이름 ")).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"이미 소개된 공간이에요\",\"slug\":\"quiet-hill\"}");
	}

	@Test
	void sameNameAsUnpublishedPlaceIsAccepted() {
		Cookie me = devLogin("me@test.jieuni");
		data.place("draft-one", PlaceStatus.DRAFT, null);

		assertThat(suggestPlace(me, "draft-one 이름")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void placeSuggestionValidation() {
		Cookie me = devLogin("me@test.jieuni");

		var json = assertThat(mvc.post().uri("/api/suggestions/places").cookie(me).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\",\"region\":\"서울\",\"link\":\"javascript:alert(1)\",\"reason\":\"" + "가".repeat(301) + "\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson();
		json.extractingPath("$.fieldErrors.name").isEqualTo("공간 이름을 입력해 주세요");
		json.extractingPath("$.fieldErrors.link").isEqualTo("http로 시작하는 링크를 넣어 주세요");
		json.extractingPath("$.fieldErrors.reason").isEqualTo("300자까지 쓸 수 있어요");
	}

	@Test
	void creditSuggestionIsPending() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(suggestCredit(me, place)).hasStatus(HttpStatus.CREATED).bodyJson()
				.extractingPath("$.status").isEqualTo("pending");
		var row = data.jdbc().sql("select role, maker_name, source_url, is_self, status from credit_suggestions where place_id = :id")
				.param("id", place).query().singleRow();
		assertThat(row).containsEntry("role", "lighting").containsEntry("maker_name", "빛 설계자")
				.containsEntry("is_self", false).containsEntry("status", "pending");
	}

	@Test
	void creditSuggestionForUnpublishedOrUnknownPlaceIs404() {
		Cookie me = devLogin("me@test.jieuni");
		long draft = data.place("draft-one", PlaceStatus.DRAFT, null);

		assertThat(suggestCredit(me, draft)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(suggestCredit(me, 999_999)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void creditSuggestionValidation() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(suggestCredit(me, place, """
				{"placeId":%d,"role":"lighting","makerName":" ","sourceUrl":"ftp://x"}"""))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors").asMap().containsOnlyKeys("makerName", "sourceUrl");
		assertThat(suggestCredit(me, place, """
				{"placeId":%d,"role":"singer","makerName":"누군가"}""")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void eleventhSuggestionOfTheDayIs429AcrossBothKinds() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);
		nowKst("2026-10-07T23:00");
		for (int i = 0; i < 5; i++) {
			assertThat(suggestPlace(me, "제보 " + i)).hasStatus(HttpStatus.CREATED);
			assertThat(suggestCredit(me, place)).hasStatus(HttpStatus.CREATED);
		}

		assertThat(suggestPlace(me, "열한 번째")).hasStatus(HttpStatus.TOO_MANY_REQUESTS).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"오늘은 더 보낼 수 없어요. 내일 다시 알려 주세요\"}");
		assertThat(suggestCredit(me, place)).hasStatus(HttpStatus.TOO_MANY_REQUESTS);

		// KST 자정이 지나면 다시 보낼 수 있다(UTC로는 같은 날 15:00).
		nowKst("2026-10-08T00:00");
		assertThat(suggestPlace(me, "다음 날")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void limitIsPerUser() {
		Cookie me = devLogin("me@test.jieuni");
		Cookie other = devLogin("other@test.jieuni");
		for (int i = 0; i < 10; i++) {
			assertThat(suggestPlace(me, "제보 " + i)).hasStatus(HttpStatus.CREATED);
		}

		assertThat(suggestPlace(other, "다른 사람")).hasStatus(HttpStatus.CREATED);
	}

	@Test
	void mySuggestionsShowOnlyMine() {
		Cookie me = devLogin("me@test.jieuni");
		Cookie other = devLogin("other@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);
		suggestPlace(me, "내 제보");
		suggestPlace(other, "남의 제보");
		suggestCredit(me, place);
		suggestCredit(other, place);

		var json = assertThat(mvc.get().uri("/api/me/suggestions").cookie(me)).hasStatusOk().bodyJson();
		json.extractingPath("$.places[*].name").asArray().containsExactly("내 제보");
		json.extractingPath("$.places[0].status").isEqualTo("suggested");
		json.extractingPath("$.places[0].region").isEqualTo("서울 종로구");
		json.extractingPath("$.credits[*].placeName").asArray().containsExactly("quiet-hill 이름");
		json.extractingPath("$.credits[0].role").isEqualTo("lighting");
		json.extractingPath("$.credits[0].status").isEqualTo("pending");
	}

	@Test
	void mySuggestionsRunTwoQueries() {
		Cookie me = devLogin("me@test.jieuni");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);
		for (int i = 0; i < 3; i++) {
			suggestPlace(me, "제보 " + i);
			suggestCredit(me, place);
		}
		// 앞서 쌓인 INSERT가 세어지지 않게 내보내고, 실제 요청처럼 빈 영속성 컨텍스트에서 잰다.
		em.flush();
		em.clear();

		statistics().clear();
		assertThat(mvc.get().uri("/api/me/suggestions").cookie(me)).hasStatusOk();
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

	@Test
	void anonymousIs401() {
		assertThat(mvc.post().uri("/api/suggestions/places").with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}")).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/me/suggestions")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

}
