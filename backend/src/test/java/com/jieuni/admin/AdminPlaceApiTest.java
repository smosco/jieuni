package com.jieuni.admin;

import com.jayway.jsonpath.JsonPath;
import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPlaceApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni"; // application-test.yml의 ADMIN_EMAILS
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	@Autowired
	EntityManager em;

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	private MvcTestResult create(String body) {
		return mvc.post().uri("/api/admin/places").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private long createdId(String name) throws UnsupportedEncodingException {
		MvcTestResult result = create("{\"name\":\"" + name + "\",\"region\":\"서울 종로구\"}");
		assertThat(result).hasStatus(HttpStatus.CREATED);
		return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
	}

	private MvcTestResult patch(long id, String body) {
		return mvc.patch().uri("/api/admin/places/" + id).cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private static String editBody(String slug) {
		return """
				{"slug":%s,"name":"빛의 정원","kind":"garden","region":"경기 양평군","address":"양평읍 1",
				 "lat":37.5,"lng":127.6,"access":"ticket","visitNote":"월요일 휴무","summary":"한 문장",
				 "impression":"감상","senses":{"light":"오후 빛","sound":null,"scent":"","touch":null},
				 "essay":"# 에세이","visitedByEditorOn":"2026-05"}"""
				.formatted(slug == null ? "null" : "\"" + slug + "\"");
	}

	private void touchedAt(long id, String instant) {
		data.jdbc().sql("update places set updated_at = :at where id = :id")
				.param("at", Instant.parse(instant).atOffset(ZoneOffset.UTC)).param("id", id).update();
	}

	@Test
	void userIs403AndAnonymousIs401() {
		Cookie user = devLogin("a@test.jieuni");

		assertThat(mvc.get().uri("/api/admin/places").cookie(user)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.post().uri("/api/admin/places").cookie(user).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"x\"}")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.get().uri("/api/admin/places/1")).hasStatus(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void createMakesDraftWithoutSlug() {
		var json = assertThat(create("{\"name\":\" 빛의 정원 \"}")).hasStatus(HttpStatus.CREATED).bodyJson();
		json.extractingPath("$.status").isEqualTo("draft");
		json.extractingPath("$.name").isEqualTo("빛의 정원");
		json.extractingPath("$.slug").isNull();
		json.extractingPath("$.region").isNull();
		json.extractingPath("$.slugLocked").isEqualTo(false);
		json.extractingPath("$.credits").asArray().isEmpty();

		assertThat(mvc.get().uri("/api/admin/places?status=draft").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].name").asArray().containsExactly("빛의 정원");
	}

	@Test
	void createValidation() {
		assertThat(create("{\"name\":\"\",\"region\":\"" + "가".repeat(51) + "\"}")).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.fieldErrors").asMap().containsOnlyKeys("name", "region");
	}

	@Test
	void getHasEveryChildListAndAdminFields() {
		long maker = data.maker("maker-a");
		long place = data.place("quiet-hill", PlaceStatus.PUBLISHED, PUBLISHED);
		data.credit(place, maker, CreditRole.ARCHITECTURE, 0);
		data.photo(place, 0);
		data.jdbc().sql("insert into place_sources (place_id, title, url, type, position) values (:id, '기사', 'https://example.com/a', 'article', 0)")
				.param("id", place).update();
		data.jdbc().sql("insert into place_embeds (place_id, type, url, position) values (:id, 'youtube', 'https://youtu.be/x', 0)")
				.param("id", place).update();
		devLogin("suggester@test.jieuni");
		data.jdbc().sql("""
				update places set suggested_by = (select id from users where provider_id = 'suggester@test.jieuni'),
				  suggestion_reason = '빛이 오래 남는다', suggestion_link = 'https://example.com/s', suggestion_maker_hint = '이로재'
				where id = :id""").param("id", place).update();
		long other = data.place("other", PlaceStatus.PUBLISHED, PUBLISHED);
		data.credit(other, maker, CreditRole.INTERIOR, 0);

		var json = assertThat(mvc.get().uri("/api/admin/places/" + place).cookie(admin)).hasStatusOk().bodyJson();
		json.extractingPath("$.credits[0].makerId").isEqualTo((int) maker);
		json.extractingPath("$.credits[0].maker.slug").isEqualTo("maker-a");
		json.extractingPath("$.credits[0].role").isEqualTo("architecture");
		json.extractingPath("$.photos[0].objectKey").isEqualTo("places/" + place + "/p0");
		json.extractingPath("$.photos[0].position").isEqualTo(0);
		json.extractingPath("$.photos[0].id").isNotNull();
		json.extractingPath("$.photos[0].url.w1600").isEqualTo("https://photos.test/places/" + place + "/p0-1600.webp");
		json.extractingPath("$.sources[0].title").isEqualTo("기사");
		json.extractingPath("$.embeds[0].type").isEqualTo("youtube");
		json.extractingPath("$.suggestedBy.nickname").isEqualTo("suggester");
		json.extractingPath("$.suggestionReason").isEqualTo("빛이 오래 남는다");
		json.extractingPath("$.suggestionLink").isEqualTo("https://example.com/s");
		json.extractingPath("$.suggestionMakerHint").isEqualTo("이로재");
		json.extractingPath("$.mapLinks.kakao").asString().startsWith("https://map.kakao.com/");
		json.extractingPath("$.relatedPlaces[*].slug").asArray().containsExactly("other");
		json.extractingPath("$.slugLocked").isEqualTo(true);
		json.extractingPath("$.status").isEqualTo("published");
	}

	@Test
	void unknownIdIs404() {
		assertThat(mvc.get().uri("/api/admin/places/999999").cookie(admin)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(patch(999_999, editBody(null))).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void patchOverwritesEveryScalarField() throws UnsupportedEncodingException {
		long id = createdId("처음 이름");
		assertThat(patch(id, editBody("light-garden"))).hasStatusOk();

		// 두 번째 저장에서 빠진 필드는 비워진다.
		var json = assertThat(patch(id, """
				{"slug":"light-garden","name":"빛의 정원","kind":"cafe","summary":"  다시 쓴 한 문장 ","visitedByEditorOn":"2026-05"}"""))
				.hasStatusOk().bodyJson();
		json.extractingPath("$.name").isEqualTo("빛의 정원");
		json.extractingPath("$.kind").isEqualTo("cafe");
		json.extractingPath("$.summary").isEqualTo("다시 쓴 한 문장");
		json.extractingPath("$.address").isNull();
		json.extractingPath("$.lat").isNull();
		json.extractingPath("$.senses.light").isNull();
		json.extractingPath("$.visitedByEditorOn").isEqualTo("2026-05");
	}

	@Test
	void patchStoresValuesAndBlankBecomesNull() throws UnsupportedEncodingException {
		long id = createdId("처음 이름");

		var json = assertThat(patch(id, editBody("light-garden"))).hasStatusOk().bodyJson();
		json.extractingPath("$.slug").isEqualTo("light-garden");
		json.extractingPath("$.access").isEqualTo("ticket");
		json.extractingPath("$.lat").isEqualTo(37.5);
		json.extractingPath("$.senses.light").isEqualTo("오후 빛");
		json.extractingPath("$.senses.scent").isNull();
		json.extractingPath("$.essay").isEqualTo("# 에세이");
		json.extractingPath("$.visitedByEditorOn").isEqualTo("2026-05");
		assertThat(data.jdbc().sql("select visited_by_editor_on from places where id = :id").param("id", id)
				.query(String.class).single()).isEqualTo("2026-05-01");
	}

	@Test
	void patchFormatValidation() throws UnsupportedEncodingException {
		long id = createdId("처음 이름");

		var json = assertThat(patch(id, """
				{"slug":"Light Garden","name":" ","summary":"%s","lat":91,"senses":{"touch":"%s"}}"""
				.formatted("가".repeat(61), "가".repeat(61)))).hasStatus(HttpStatus.BAD_REQUEST).bodyJson();
		json.extractingPath("$.fieldErrors").asMap()
				.containsOnlyKeys("slug", "name", "summary", "lat", "senses.touch");
		json.extractingPath("$.fieldErrors.slug").isEqualTo("영문 소문자·숫자·하이픈만 쓸 수 있어요");

		assertThat(patch(id, "{\"name\":\"이름\",\"kind\":\"castle\"}")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(patch(id, "{\"name\":\"이름\",\"visitedByEditorOn\":\"2026-13\"}")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void blankSlugBecomesNullAndSlugIsStrippedBeforeFormatCheck() throws UnsupportedEncodingException {
		long id = createdId("작성 중");

		assertThat(patch(id, editBody(""))).hasStatusOk().bodyJson().extractingPath("$.slug").isNull();
		assertThat(patch(id, editBody(" light-garden "))).hasStatusOk().bodyJson()
				.extractingPath("$.slug").isEqualTo("light-garden");
	}

	@Test
	void slugCanChangeBeforePublishButNotAfter() throws UnsupportedEncodingException {
		long draft = createdId("작성 중");
		assertThat(patch(draft, editBody("first"))).hasStatusOk();
		assertThat(patch(draft, editBody("second"))).hasStatusOk();
		assertThat(patch(draft, editBody(null))).hasStatusOk();

		long published = data.place("published-one", PlaceStatus.PUBLISHED, PUBLISHED);
		assertThat(patch(published, editBody("published-one"))).hasStatusOk();
		assertThat(patch(published, editBody("renamed"))).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"공개한 뒤에는 주소(slug)를 바꿀 수 없어요\"}");
		assertThat(patch(published, editBody(null))).hasStatus(HttpStatus.CONFLICT);
		assertThat(patch(published, editBody(""))).hasStatus(HttpStatus.CONFLICT);
	}

	// DB 유니크 제약이 409로 바뀌는지 본다. 제약 위반 뒤 PostgreSQL이 테스트 트랜잭션을 중단하므로 이 요청이 마지막 동작이다.
	@Test
	void duplicateSlugIs409() throws UnsupportedEncodingException {
		data.place("taken", PlaceStatus.DRAFT, null);
		long id = createdId("새 공간");

		assertThat(patch(id, editBody("taken"))).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"이미 다른 공간이 쓰는 주소(slug)예요\"}");
	}

	@Test
	void listIsNewestUpdatedFirstWithStatusAndNameFilter() {
		long a = data.place("a", PlaceStatus.PUBLISHED, PUBLISHED);
		long b = data.place("b", PlaceStatus.DRAFT, null);
		long c = data.place("c", PlaceStatus.SUGGESTED, null);
		data.jdbc().sql("update places set name = '바람 정원' where id = :id").param("id", c).update();
		touchedAt(a, "2026-10-01T00:00:00Z");
		touchedAt(b, "2026-10-03T00:00:00Z");
		touchedAt(c, "2026-10-02T00:00:00Z");
		data.photo(a, 0);

		var all = assertThat(mvc.get().uri("/api/admin/places").cookie(admin)).hasStatusOk().bodyJson();
		all.extractingPath("$.items[*].slug").asArray().containsExactly("b", "c", "a");
		all.extractingPath("$.items[2].hasPhoto").isEqualTo(true);
		all.extractingPath("$.items[0].hasPhoto").isEqualTo(false);
		all.extractingPath("$.items[0].updatedAt").isEqualTo("2026-10-03T00:00:00Z");
		all.extractingPath("$.items[1].status").isEqualTo("suggested");

		assertThat(mvc.get().uri("/api/admin/places?status=published").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().containsExactly("a");
		assertThat(mvc.get().uri("/api/admin/places?q=정원").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].name").asArray().containsExactly("바람 정원");
		assertThat(mvc.get().uri("/api/admin/places?q=A 이름").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().containsExactly("a");
		assertThat(mvc.get().uri("/api/admin/places?q=%25").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.items").asArray().isEmpty();
	}

	@Test
	void unknownStatusIs400() {
		assertThat(mvc.get().uri("/api/admin/places?status=hidden").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST)
				.bodyJson().extractingPath("$.message").isEqualTo("알 수 없는 상태예요: hidden");
	}

	@Test
	void cursorWalksToTheEndWithoutDuplicates() throws UnsupportedEncodingException {
		// 수정 시각이 같은 두 곳은 id로 순서가 갈린다.
		List<Long> ids = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			long id = data.place("p" + i, PlaceStatus.DRAFT, null);
			touchedAt(id, i < 2 ? "2026-10-01T00:00:00Z" : "2026-10-0" + (i + 1) + "T00:00:00Z");
			ids.add(id);
		}
		List<String> expected = List.of("p4", "p3", "p2", "p1", "p0");

		List<String> seen = new ArrayList<>();
		String cursor = null;
		do {
			MvcTestResult result = mvc.get().uri("/api/admin/places?size=2" + (cursor == null ? "" : "&cursor=" + cursor))
					.cookie(admin).exchange();
			String body = result.getResponse().getContentAsString();
			seen.addAll(JsonPath.read(body, "$.items[*].slug"));
			cursor = JsonPath.read(body, "$.nextCursor");
		} while (cursor != null);

		assertThat(seen).containsExactlyElementsOf(expected);
	}

	@Test
	void listRunsOneQueryAfterAdminCheck() {
		for (int i = 0; i < 5; i++) {
			long id = data.place("p" + i, PlaceStatus.DRAFT, null);
			data.photo(id, 0);
		}
		em.flush();
		em.clear();

		statistics().clear();
		assertThat(mvc.get().uri("/api/admin/places?size=3").cookie(admin)).hasStatusOk();
		// 관리자 확인(users.role) 1 · 목록 1
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

}
