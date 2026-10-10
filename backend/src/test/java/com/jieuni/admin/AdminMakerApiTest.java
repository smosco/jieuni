package com.jieuni.admin;

import com.jayway.jsonpath.JsonPath;
import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminMakerApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni"; // application-test.yml의 ADMIN_EMAILS
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	private MvcTestResult post(String body) {
		return mvc.post().uri("/api/admin/makers").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private MvcTestResult patch(long id, String body) {
		return mvc.patch().uri("/api/admin/makers/" + id).cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private static String body(String slug) {
		return "{\"name\":\"윤서진\",\"kind\":\"person\",\"slug\":" + (slug == null ? "null" : "\"" + slug + "\"") + "}";
	}

	private long makerWithoutSlug() {
		return data.jdbc().sql("""
				insert into makers (name, kind, created_at, updated_at)
				values ('이름 없는 주인', 'owner', now(), now()) returning id""").query(Long.class).single();
	}

	private void touchedAt(long id, String instant) {
		data.jdbc().sql("update makers set updated_at = :at where id = :id")
				.param("at", Instant.parse(instant).atOffset(ZoneOffset.UTC)).param("id", id).update();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> fieldErrors(MvcTestResult result) throws UnsupportedEncodingException {
		return (Map<String, String>) JsonPath.read(result.getResponse().getContentAsString(), "$.fieldErrors");
	}

	@Test
	void userIs403() {
		Cookie user = devLogin("a@test.jieuni");
		assertThat(mvc.get().uri("/api/admin/makers").cookie(user)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.post().uri("/api/admin/makers").cookie(user).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body(null))).hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void createCleansValuesAndGetReturnsThem() throws UnsupportedEncodingException {
		long studio = data.maker("atelier-on");

		MvcTestResult created = post("""
				{"name":" 윤서진 ","kind":"person","slug":" seojin-yoon ","region":"서울 종로구","intro":"",
				 "website":"https://example.com/sj","instagram":"@seojin.yoon","affiliationId":%d}""".formatted(studio));
		var json = assertThat(created).hasStatus(HttpStatus.CREATED).bodyJson();
		json.extractingPath("$.name").isEqualTo("윤서진");
		json.extractingPath("$.slug").isEqualTo("seojin-yoon");
		json.extractingPath("$.intro").isNull();
		json.extractingPath("$.instagram").isEqualTo("seojin.yoon");
		json.extractingPath("$.affiliation.slug").isEqualTo("atelier-on");
		json.extractingPath("$.places").asArray().isEmpty();
		json.extractingPath("$.slugLocked").isEqualTo(false);
		json.extractingPath("$.createdAt").isEqualTo("2026-10-07T01:00:00Z");

		long id = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();
		assertThat(mvc.get().uri("/api/admin/makers/" + id).cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.region").isEqualTo("서울 종로구");
	}

	@Test
	void createValidation() throws UnsupportedEncodingException {
		MvcTestResult result = post("""
				{"name":" ","slug":"Seojin","region":"%s","intro":"%s","website":"javascript:alert(1)","instagram":"seo jin"}"""
				.formatted("가".repeat(51), "가".repeat(201)));
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).containsOnlyKeys("name", "kind", "slug", "region", "intro", "website", "instagram");
	}

	@Test
	void affiliationMustExistAndNotBeSelf() throws UnsupportedEncodingException {
		MvcTestResult unknown = post("{\"name\":\"윤서진\",\"kind\":\"person\",\"affiliationId\":999999}");
		assertThat(unknown).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(unknown)).containsExactly(Map.entry("affiliationId", "없는 만든 사람이에요"));

		long self = data.maker("self");
		MvcTestResult selfResult = patch(self, "{\"name\":\"나\",\"kind\":\"studio\",\"slug\":\"self\",\"affiliationId\":%d}".formatted(self));
		assertThat(selfResult).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(selfResult)).containsExactly(Map.entry("affiliationId", "자기 자신은 소속이 될 수 없어요"));
	}

	@Test
	void patchOverwritesEveryField() {
		long studio = data.maker("atelier-on");
		long id = data.maker("seojin-yoon");
		data.jdbc().sql("update makers set region = '서울', intro = '소개', affiliation_id = :s where id = :id")
				.param("s", studio).param("id", id).update();

		var json = assertThat(patch(id, "{\"name\":\"새 이름\",\"kind\":\"studio\",\"slug\":\"seojin-yoon\"}"))
				.hasStatusOk().bodyJson();
		json.extractingPath("$.name").isEqualTo("새 이름");
		json.extractingPath("$.kind").isEqualTo("studio");
		json.extractingPath("$.region").isNull();
		json.extractingPath("$.intro").isNull();
		json.extractingPath("$.affiliation").isNull();
	}

	@Test
	void listFiltersByNameNewestFirstWithPlaceCount() {
		long a = data.maker("a");
		long b = data.maker("b");
		long c = data.maker("c");
		data.jdbc().sql("update makers set name = '바람 스튜디오' where id = :id").param("id", c).update();
		touchedAt(a, "2026-10-01T00:00:00Z");
		touchedAt(b, "2026-10-03T00:00:00Z");
		touchedAt(c, "2026-10-02T00:00:00Z");
		long p1 = data.place("p1", PlaceStatus.PUBLISHED, PUBLISHED);
		long p2 = data.place("p2", PlaceStatus.DRAFT, null);
		data.credit(p1, a, CreditRole.ARCHITECTURE, 0);
		data.credit(p1, a, CreditRole.INTERIOR, 1);
		data.credit(p2, a, CreditRole.OWNER, 0);

		var all = assertThat(mvc.get().uri("/api/admin/makers").cookie(admin)).hasStatusOk().bodyJson();
		all.extractingPath("$.items[*].id").asArray().containsExactly((int) b, (int) c, (int) a);
		all.extractingPath("$.items[2].placeCount").isEqualTo(2);
		all.extractingPath("$.items[0].placeCount").isEqualTo(0);

		assertThat(mvc.get().uri("/api/admin/makers?q=바람").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].id").asArray().containsExactly((int) c);

		var first = assertThat(mvc.get().uri("/api/admin/makers?size=1").cookie(admin)).hasStatusOk().bodyJson();
		first.extractingPath("$.items[*].id").asArray().containsExactly((int) b);
		first.extractingPath("$.nextCursor").isNotNull();
	}

	private MvcTestResult putCredits(long placeId, String body) {
		return mvc.put().uri("/api/admin/places/" + placeId + "/credits").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private static String lockedConflict() {
		return "{\"message\":\"공개된 공간에 연결된 뒤에는 주소(slug)를 바꿀 수 없어요\"}";
	}

	@Test
	void slugIsLockedWhenAPlaceCreditingThemIsFirstPublished() {
		long id = data.maker("seojin-yoon");
		long place = data.publishablePlace("garden", PlaceStatus.DRAFT, null);
		data.credit(place, id, CreditRole.INTERIOR, 1);

		// 작성 중인 공간뿐이면 프로필 주소가 아직 밖에 나가지 않았다.
		assertThat(mvc.get().uri("/api/admin/makers/" + id).cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.slugLocked").isEqualTo(false);
		assertThat(patch(id, body("seojin"))).hasStatusOk();

		assertThat(mvc.post().uri("/api/admin/places/" + place + "/status").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"published\"}")).hasStatusOk();
		var json = assertThat(mvc.get().uri("/api/admin/makers/" + id).cookie(admin)).hasStatusOk().bodyJson();
		json.extractingPath("$.slugLocked").isEqualTo(true);
		json.extractingPath("$.places[*].slug").asArray().containsExactly("garden");

		assertThat(patch(id, body("seojin"))).hasStatusOk();
		assertThat(patch(id, body("other"))).hasStatus(HttpStatus.CONFLICT).bodyJson().isStrictlyEqualTo(lockedConflict());
		assertThat(patch(id, body(null))).hasStatus(HttpStatus.CONFLICT);
	}

	// 한 번 밖에 나간 프로필 주소는 크레딧에서 빠져도 바꿀 수 없다(빼고 바꾸고 다시 넣는 길을 막는다).
	@Test
	void slugStaysLockedAfterCreditIsRemoved() {
		long id = data.maker("seojin-yoon");
		long place = data.publishablePlace("live", PlaceStatus.PUBLISHED, PUBLISHED);
		long keeper = data.jdbc().sql("select id from makers where slug = 'live-maker'").query(Long.class).single();

		assertThat(putCredits(place, """
				[{"makerId":%d,"role":"architecture","verified":true,"sourceUrl":"https://example.com/a"},
				 {"makerId":%d,"role":"interior","verified":false}]""".formatted(keeper, id))).hasStatusOk();
		assertThat(putCredits(place, """
				[{"makerId":%d,"role":"architecture","verified":true,"sourceUrl":"https://example.com/a"}]"""
				.formatted(keeper))).hasStatusOk();

		var json = assertThat(mvc.get().uri("/api/admin/makers/" + id).cookie(admin)).hasStatusOk().bodyJson();
		json.extractingPath("$.slugLocked").isEqualTo(true);
		json.extractingPath("$.places").asArray().isEmpty();
		assertThat(patch(id, body("other"))).hasStatus(HttpStatus.CONFLICT).bodyJson().isStrictlyEqualTo(lockedConflict());
	}

	// 작성 중인 공간의 크레딧 교체는 잠그지 않는다.
	@Test
	void creditsOnADraftDoNotLock() {
		long id = data.maker("seojin-yoon");
		long place = data.place("draft", PlaceStatus.DRAFT, null);

		assertThat(putCredits(place, "[{\"makerId\":%d,\"role\":\"interior\",\"verified\":false}]".formatted(id)))
				.hasStatusOk();
		assertThat(patch(id, body("other"))).hasStatusOk().bodyJson().extractingPath("$.slugLocked").isEqualTo(false);
	}

	@Test
	void firstSlugCanBeSetEvenAfterPublishedCredit() {
		long id = makerWithoutSlug();
		long live = data.place("live", PlaceStatus.PUBLISHED, PUBLISHED);
		data.credit(live, id, CreditRole.OWNER, 0);

		assertThat(patch(id, "{\"name\":\"주인\",\"kind\":\"owner\",\"slug\":\"keeper\"}")).hasStatusOk().bodyJson()
				.extractingPath("$.slugLocked").isEqualTo(true);
	}

	@Test
	void unknownIdIs404() {
		assertThat(mvc.get().uri("/api/admin/makers/999999").cookie(admin)).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(patch(999_999, body(null))).hasStatus(HttpStatus.NOT_FOUND);
	}

	// 제약 위반 뒤에는 PostgreSQL이 테스트 트랜잭션을 중단하므로 각 테스트의 마지막 요청으로 둔다.
	@Test
	void duplicateSlugOnCreateIs409() {
		data.maker("taken");
		assertThat(post(body("taken"))).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"이미 다른 만든 사람이 쓰는 주소(slug)예요\"}");
	}

	@Test
	void duplicateSlugOnPatchIs409() {
		data.maker("taken");
		long id = data.maker("mine");
		assertThat(patch(id, body("taken"))).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"이미 다른 만든 사람이 쓰는 주소(slug)예요\"}");
	}

}
