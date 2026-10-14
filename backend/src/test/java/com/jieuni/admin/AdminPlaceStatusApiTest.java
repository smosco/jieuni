package com.jieuni.admin;

import com.jayway.jsonpath.JsonPath;
import com.jieuni.IntegrationTest;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPlaceStatusApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni"; // application-test.yml의 ADMIN_EMAILS
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	@Autowired
	EntityManager em;

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	// 공개 시 필수를 모두 채운 공간. 테스트마다 하나씩 비워 본다.
	private long publishable(String slug, PlaceStatus status) {
		return data.publishablePlace(slug, status, PlaceStatus.HAS_PAGE.contains(status) ? PUBLISHED : null);
	}

	// 테스트는 요청들과 영속성 컨텍스트를 함께 쓴다. 앞 요청이 읽어 둔 공간·자식 목록이 JDBC 변경을 가리지 않게 비운다.
	private void sql(String sql, long id) {
		data.jdbc().sql(sql).param("id", id).update();
		em.clear();
	}

	private MvcTestResult status(long id, String to) {
		return mvc.post().uri("/api/admin/places/" + id + "/status").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"" + to + "\"}").exchange();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> fieldErrors(MvcTestResult result) throws UnsupportedEncodingException {
		return (Map<String, String>) JsonPath.read(result.getResponse().getContentAsString(), "$.fieldErrors");
	}

	@Test
	void publishingSetsPublishedAtAndShowsOnPublicApi() {
		long id = publishable("quiet-hill", PlaceStatus.DRAFT);
		assertThat(mvc.get().uri("/api/places/quiet-hill")).hasStatus(HttpStatus.NOT_FOUND);

		var json = assertThat(status(id, "published")).hasStatusOk().bodyJson();
		json.extractingPath("$.status").isEqualTo("published");
		json.extractingPath("$.publishedAt").isEqualTo("2026-10-07T01:00:00Z");
		json.extractingPath("$.slugLocked").isEqualTo(true);

		assertThat(mvc.get().uri("/api/places/quiet-hill")).hasStatusOk();
		assertThat(mvc.get().uri("/api/places")).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().contains("quiet-hill");
	}

	@Test
	void eachMissingRequiredFieldIs400WithThatKey() throws UnsupportedEncodingException {
		Map<String, String> cases = new LinkedHashMap<>();
		cases.put("slug = null", "slug");
		cases.put("name = ''", "name");
		cases.put("kind = null", "kind");
		cases.put("region = null", "region");
		cases.put("address = null", "address");
		cases.put("access = null", "access");
		cases.put("summary = null", "summary");
		cases.put("impression = null", "impression");
		int i = 0;
		for (var c : cases.entrySet()) {
			long id = publishable("missing-" + i++, PlaceStatus.DRAFT);
			sql("update places set " + c.getKey() + " where id = :id", id);

			MvcTestResult result = status(id, "published");
			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
					.extractingPath("$.message").isEqualTo("공개할 수 없어요");
			assertThat(fieldErrors(result)).as(c.getKey()).containsOnlyKeys(c.getValue());
		}
	}

	@Test
	void creditNeedsSourceButNotVerified() throws UnsupportedEncodingException {
		long id = publishable("credit", PlaceStatus.DRAFT);
		sql("update place_credits set verified = false, source_url = null where place_id = :id", id);
		MvcTestResult result = status(id, "published");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).isEqualTo(Map.of("credits", "출처가 있는 크레딧이 1개 이상 필요해요"));

		// 확인 전이어도 출처가 있으면 된다.
		sql("update place_credits set source_url = 'https://example.com/c' where place_id = :id", id);
		assertThat(status(id, "published")).hasStatusOk();
	}

	@Test
	void photoOrEmbedIsNeeded() throws UnsupportedEncodingException {
		long id = publishable("media", PlaceStatus.DRAFT);
		sql("delete from place_photos where place_id = :id", id);
		MvcTestResult result = status(id, "published");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).containsOnlyKeys("photos");

		sql("insert into place_embeds (place_id, type, url, position) values (:id, 'instagram', 'https://instagram.com/p/x', 0)", id);
		assertThat(status(id, "published")).hasStatusOk();
	}

	@Test
	void sourceIsNeededWhenEditorHasNotVisited() throws UnsupportedEncodingException {
		long id = publishable("visit", PlaceStatus.DRAFT);
		sql("update places set visited_by_editor_on = null where id = :id", id);
		MvcTestResult result = status(id, "published");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).containsOnlyKeys("sources");

		sql("insert into place_sources (place_id, title, url, type, position) values (:id, '기사', 'https://example.com/a', 'article', 0)", id);
		assertThat(status(id, "published")).hasStatusOk();
	}

	@Test
	void everyPhotoNeedsAltCreditAndSourceUnlessOwn() throws UnsupportedEncodingException {
		long id = publishable("photos", PlaceStatus.DRAFT);
		data.photo(id, 1);
		sql("update place_photos set alt = null, credit = ' ' where place_id = :id and position = 0", id);
		sql("update place_photos set license = 'credited', source_url = null where place_id = :id and position = 1", id);

		MvcTestResult result = status(id, "published");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).isEqualTo(Map.of(
				"photos[0].alt", "사진 설명을 입력해 주세요",
				"photos[0].credit", "사진 출처(이름)를 입력해 주세요",
				"photos[1].sourceUrl", "출처 링크를 입력해 주세요"));
	}

	// 좌표는 공개 조건이 아니다.
	@Test
	void publishesWithoutCoordinates() {
		long id = publishable("no-coords", PlaceStatus.DRAFT);
		sql("update places set lat = null, lng = null where id = :id", id);

		assertThat(status(id, "published")).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("published");
	}

	@Test
	void manyErrorsComeTogetherInFormOrder() throws UnsupportedEncodingException {
		long id = publishable("many", PlaceStatus.DRAFT);
		sql("update places set address = null where id = :id", id);
		sql("update place_credits set verified = false, source_url = null where place_id = :id", id);

		MvcTestResult result = status(id, "published");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(new ArrayList<>(fieldErrors(result).keySet())).containsExactly("address", "credits");
	}

	@Test
	void onlyTransitionsInTheTableAreAllowed() {
		Map<PlaceStatus, Set<PlaceStatus>> allowed = Map.of(
				PlaceStatus.SUGGESTED, Set.of(PlaceStatus.DRAFT, PlaceStatus.REJECTED),
				PlaceStatus.DRAFT, Set.of(PlaceStatus.PUBLISHED),
				PlaceStatus.PUBLISHED, Set.of(PlaceStatus.PAUSED, PlaceStatus.CLOSED),
				PlaceStatus.PAUSED, Set.of(PlaceStatus.PUBLISHED, PlaceStatus.CLOSED));
		int i = 0;
		for (PlaceStatus from : PlaceStatus.values()) {
			for (PlaceStatus to : EnumSet.allOf(PlaceStatus.class)) {
				long id = publishable("t-" + i++, from);
				boolean ok = allowed.getOrDefault(from, Set.of()).contains(to);
				assertThat(status(id, to.value()).getResponse().getStatus()).as(from + " → " + to)
						.isEqualTo(ok ? 200 : 409);
			}
		}
	}

	@Test
	void publishedToDraftIs409WithMessage() {
		long id = publishable("back", PlaceStatus.PUBLISHED);
		assertThat(status(id, "draft")).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.isStrictlyEqualTo("{\"message\":\"공개 상태에서는 작성 중(으)로 바꿀 수 없어요\"}");
	}

	@Test
	void republishingAfterPauseKeepsFirstPublishedAt() {
		long id = publishable("pause", PlaceStatus.DRAFT);
		assertThat(status(id, "published")).hasStatusOk();
		clock.set(DEFAULT_NOW.plus(1, ChronoUnit.DAYS));
		assertThat(status(id, "paused")).hasStatusOk().bodyJson().extractingPath("$.status").isEqualTo("paused");

		assertThat(status(id, "published")).hasStatusOk().bodyJson()
				.extractingPath("$.publishedAt").isEqualTo("2026-10-07T01:00:00Z");
	}

	@Test
	void closedLeavesBrowseButKeepsDetailPage() {
		long id = publishable("closing", PlaceStatus.PUBLISHED);
		assertThat(status(id, "closed")).hasStatusOk();

		assertThat(mvc.get().uri("/api/places")).hasStatusOk().bodyJson()
				.extractingPath("$.items[*].slug").asArray().doesNotContain("closing");
		assertThat(mvc.get().uri("/api/places/closing")).hasStatusOk().bodyJson()
				.extractingPath("$.status").isEqualTo("closed");
	}

	private MvcTestResult patchWithoutAddress(long id, String slug) {
		return mvc.patch().uri("/api/admin/places/" + id).cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"slug":"%s","name":"%s 이름","kind":"garden","region":"서울 종로구","address":"",
						 "access":"ticket","summary":"한 문장","impression":"감상",
						 "visitedByEditorOn":"2026-05"}""".formatted(slug, slug))
				.exchange();
	}

	private String addressInDb(long id) {
		return data.jdbc().sql("select address from places where id = :id").param("id", id).query(String.class).single();
	}

	// 실패한 요청은 롤백된다. 테스트는 트랜잭션 하나를 함께 쓰므로 바뀐 엔티티가 남지 않게 JDBC로 바로 확인하고 끝낸다.
	@Test
	void patchOnPublishedPlaceMustStayPublishable() throws UnsupportedEncodingException {
		long id = publishable("live", PlaceStatus.PUBLISHED);

		MvcTestResult result = patchWithoutAddress(id, "live");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.message").isEqualTo("공개된 공간이라 이 값은 비울 수 없어요");
		assertThat(fieldErrors(result)).containsOnlyKeys("address");
		assertThat(addressInDb(id)).isEqualTo("서울 종로구 1");
	}

	@Test
	void patchOnClosedPlaceMustStayPublishable() {
		long id = publishable("gone", PlaceStatus.CLOSED);
		assertThat(patchWithoutAddress(id, "gone")).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void patchOnDraftIsOnlyFormatChecked() {
		long id = publishable("draft", PlaceStatus.DRAFT);
		assertThat(patchWithoutAddress(id, "draft")).hasStatusOk().bodyJson().extractingPath("$.address").isNull();
	}

	@Test
	void badRequests() {
		long id = publishable("bad", PlaceStatus.DRAFT);
		assertThat(status(id, "hidden")).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.post().uri("/api/admin/places/" + id + "/status").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson().extractingPath("$.fieldErrors.to").isEqualTo("바꿀 상태를 골라 주세요");
		assertThat(status(999_999, "published")).hasStatus(HttpStatus.NOT_FOUND);

		Cookie user = devLogin("a@test.jieuni");
		assertThat(mvc.post().uri("/api/admin/places/" + id + "/status").cookie(user).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"to\":\"published\"}")).hasStatus(HttpStatus.FORBIDDEN);
	}

}
