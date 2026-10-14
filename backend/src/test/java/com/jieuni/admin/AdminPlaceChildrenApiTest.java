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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPlaceChildrenApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni"; // application-test.yml의 ADMIN_EMAILS
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	@Autowired
	EntityManager em;

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	private MvcTestResult put(long id, String list, String body) {
		return mvc.put().uri("/api/admin/places/" + id + "/" + list).cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private long publishable(String slug, PlaceStatus status) {
		return data.publishablePlace(slug, status, PlaceStatus.HAS_PAGE.contains(status) ? PUBLISHED : null);
	}

	private int count(String table, long placeId) {
		return data.jdbc().sql("select count(*) from " + table + " where place_id = :id").param("id", placeId)
				.query(Integer.class).single();
	}

	@SuppressWarnings("unchecked")
	private static Map<String, String> fieldErrors(MvcTestResult result) throws UnsupportedEncodingException {
		return (Map<String, String>) JsonPath.read(result.getResponse().getContentAsString(), "$.fieldErrors");
	}

	@Test
	void userIs403() {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		Cookie user = devLogin("a@test.jieuni");
		for (String list : new String[] {"credits", "sources", "embeds"}) {
			assertThat(mvc.put().uri("/api/admin/places/" + place + "/" + list).cookie(user).with(xsrf())
					.contentType(MediaType.APPLICATION_JSON).content("[]")).hasStatus(HttpStatus.FORBIDDEN);
		}
	}

	@Test
	void creditsAreReplacedInArrayOrder() {
		long a = data.maker("maker-a");
		long b = data.maker("maker-b");
		long place = data.place("p", PlaceStatus.DRAFT, null);
		data.credit(place, a, CreditRole.OWNER, 0);

		var json = assertThat(put(place, "credits", """
				[{"makerId":%d,"role":"architecture","verified":false},
				 {"makerId":%d,"role":"interior","verified":true,"sourceUrl":" https://example.com/b "},
				 {"makerId":%d,"role":"lighting","verified":false,"sourceUrl":""}]""".formatted(b, a, b)))
				.hasStatusOk().bodyJson();
		json.extractingPath("$.credits[*].makerId").asArray().containsExactly((int) b, (int) a, (int) b);
		json.extractingPath("$.credits[*].role").asArray().containsExactly("architecture", "interior", "lighting");
		json.extractingPath("$.credits[*].position").asArray().containsExactly(0, 1, 2);
		json.extractingPath("$.credits[1].sourceUrl").isEqualTo("https://example.com/b");
		json.extractingPath("$.credits[2].sourceUrl").isNull();
		assertThat(count("place_credits", place)).isEqualTo(3);

		em.clear();
		assertThat(mvc.get().uri("/api/admin/places/" + place).cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.credits[*].role").asArray().containsExactly("architecture", "interior", "lighting");

		assertThat(put(place, "credits", "[]")).hasStatusOk().bodyJson().extractingPath("$.credits").asArray().isEmpty();
		assertThat(count("place_credits", place)).isZero();
	}

	@Test
	void verifiedCreditNeedsSourceAndMakerMustExist() throws UnsupportedEncodingException {
		long a = data.maker("maker-a");
		long place = data.place("p", PlaceStatus.DRAFT, null);
		data.credit(place, a, CreditRole.OWNER, 0);

		MvcTestResult result = put(place, "credits", """
				[{"makerId":%d,"role":"architecture","verified":false},
				 {"makerId":%d,"role":"interior","verified":true,"sourceUrl":"  "},
				 {"makerId":999999,"role":"lighting","verified":false}]""".formatted(a, a));
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.message").isEqualTo("입력한 값을 확인해 주세요");
		assertThat(fieldErrors(result)).containsExactly(
				Map.entry("credits[1].sourceUrl", "확인됨이면 출처 링크가 필요해요"),
				Map.entry("credits[2].makerId", "없는 만든 사람이에요"));
		assertThat(count("place_credits", place)).isEqualTo(1);
	}

	@Test
	void formatErrorsComeWithIndexedKeys() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);

		MvcTestResult credits = put(place, "credits", """
				[{"makerId":1,"role":"architecture","verified":false},
				 {"verified":false,"sourceUrl":"javascript:alert(1)"}]""");
		assertThat(credits).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(credits)).containsOnlyKeys("credits[1].makerId", "credits[1].role", "credits[1].sourceUrl");

		MvcTestResult sources = put(place, "sources", """
				[{"title":"","url":"http://example.com","type":null}]""");
		assertThat(sources).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(sources)).containsExactlyInAnyOrderEntriesOf(Map.of(
				"sources[0].title", "제목을 입력해 주세요",
				"sources[0].url", "https://로 시작하는 주소를 넣어 주세요",
				"sources[0].type", "종류를 골라 주세요"));

		MvcTestResult embeds = put(place, "embeds", "[null]");
		assertThat(embeds).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(embeds)).containsExactly(Map.entry("embeds[0]", "빈 항목은 보낼 수 없어요"));

		// 모르는 enum 값은 JSON을 읽지 못한 것이라 칸 이름 없는 400이다(PATCH와 같다).
		assertThat(put(place, "sources", "[{\"title\":\"t\",\"url\":\"https://e.com\",\"type\":\"blog\"}]"))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson().isStrictlyEqualTo("{\"message\":\"요청을 처리할 수 없어요\"}");
	}

	@Test
	void sourcesAndEmbedsAreReplacedInArrayOrder() {
		long place = data.place("p", PlaceStatus.DRAFT, null);

		var sources = assertThat(put(place, "sources", """
				[{"title":" 기사 ","url":"https://example.com/a","type":"article"},
				 {"title":"공식","url":"https://example.com/o","type":"official"}]""")).hasStatusOk().bodyJson();
		sources.extractingPath("$.sources[*].title").asArray().containsExactly("기사", "공식");
		sources.extractingPath("$.sources[*].type").asArray().containsExactly("article", "official");

		var embeds = assertThat(put(place, "embeds", """
				[{"type":"instagram","url":"https://www.instagram.com/p/abc/"},
				 {"type":"youtube","url":"https://youtu.be/xyz"}]""")).hasStatusOk().bodyJson();
		embeds.extractingPath("$.embeds[*].type").asArray().containsExactly("instagram", "youtube");
		assertThat(count("place_sources", place)).isEqualTo(2);
		assertThat(count("place_embeds", place)).isEqualTo(2);

		assertThat(put(place, "sources", "[{\"title\":\"하나\",\"url\":\"https://example.com/1\",\"type\":\"video\"}]"))
				.hasStatusOk().bodyJson().extractingPath("$.sources[*].title").asArray().containsExactly("하나");
		assertThat(count("place_sources", place)).isEqualTo(1);
	}

	@Test
	void embedUrlMustBelongToItsType() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);

		MvcTestResult result = put(place, "embeds", """
				[{"type":"youtube","url":"https://www.instagram.com/p/abc/"},
				 {"type":"instagram","url":"https://evil.example.com/p/abc/"},
				 {"type":"youtube","url":"https://www.youtube.com/watch?v=ok"}]""");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).containsExactly(
				Map.entry("embeds[0].url", "유튜브 영상 주소를 넣어 주세요"),
				Map.entry("embeds[1].url", "인스타그램 게시물 주소를 넣어 주세요"));
		assertThat(count("place_embeds", place)).isZero();
	}

	// 도메인이 맞아도 공개 화면이 그리지 못하는 주소(프로필·홈·채널)로는 공개 조건을 채울 수 없다.
	@Test
	void embedUrlMustPointToAVideoOrPost() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);

		MvcTestResult result = put(place, "embeds", """
				[{"type":"instagram","url":"https://www.instagram.com/someone/"},
				 {"type":"instagram","url":"https://www.instagram.com/"},
				 {"type":"youtube","url":"https://www.youtube.com/"},
				 {"type":"youtube","url":"https://www.youtube.com/@channel"},
				 {"type":"youtube","url":"https://www.youtube.com/watch?v="},
				 {"type":"youtube","url":"https://youtu.be/"}]""");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).containsOnlyKeys("embeds[0].url", "embeds[1].url", "embeds[2].url",
				"embeds[3].url", "embeds[4].url", "embeds[5].url");

		assertThat(put(place, "embeds", """
				[{"type":"instagram","url":"https://www.instagram.com/p/abc/"},
				 {"type":"instagram","url":"https://instagram.com/reel/abc"},
				 {"type":"youtube","url":"https://www.youtube.com/watch?v=abc&t=10"},
				 {"type":"youtube","url":"https://m.youtube.com/shorts/abc"},
				 {"type":"youtube","url":"https://www.youtube.com/embed/abc"},
				 {"type":"youtube","url":"https://youtu.be/abc"}]""")).hasStatusOk();
		assertThat(count("place_embeds", place)).isEqualTo(6);
	}

	@Test
	void replacingBumpsPlaceUpdatedAt() {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		clock.set(Instant.parse("2026-10-08T03:00:00Z"));

		assertThat(put(place, "sources", "[]")).hasStatusOk().bodyJson()
				.extractingPath("$.updatedAt").isEqualTo("2026-10-08T03:00:00Z");
	}

	@Test
	void publishedPlaceKeepsWorkingWhenStillPublishable() {
		long place = publishable("live", PlaceStatus.PUBLISHED);
		long other = data.maker("other");

		assertThat(put(place, "credits", """
				[{"makerId":%d,"role":"landscape","verified":false,"sourceUrl":"https://example.com/o"}]""".formatted(other)))
				.hasStatusOk();
	}

	// 실패한 요청은 롤백된다. 테스트는 트랜잭션 하나를 함께 쓰므로 바뀐 엔티티가 남지 않게 JDBC로 바로 확인하고 끝낸다.
	@Test
	void publishedPlaceCannotLoseItsLastSourcedCredit() throws UnsupportedEncodingException {
		long place = publishable("live", PlaceStatus.PUBLISHED);
		long other = data.maker("other");

		MvcTestResult result = put(place, "credits", """
				[{"makerId":%d,"role":"landscape","verified":false}]""".formatted(other));
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.message").isEqualTo("공개된 공간이라 이 값은 비울 수 없어요");
		assertThat(fieldErrors(result)).containsExactly(Map.entry("credits", "출처가 있는 크레딧이 1개 이상 필요해요"));
		assertThat(data.jdbc().sql("select maker_id from place_credits where place_id = :id").param("id", place)
				.query(Long.class).list()).hasSize(1).doesNotContain(other);
	}

	@Test
	void closedPlaceCannotLoseItsLastEmbedOrPhoto() throws UnsupportedEncodingException {
		long place = publishable("gone", PlaceStatus.CLOSED);
		data.jdbc().sql("delete from place_photos where place_id = :id").param("id", place).update();
		data.jdbc().sql("insert into place_embeds (place_id, type, url, position) values (:id, 'youtube', 'https://youtu.be/x', 0)")
				.param("id", place).update();

		MvcTestResult result = put(place, "embeds", "[]");
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).containsExactly(Map.entry("photos", "사진 또는 임베드가 1개 이상 필요해요"));
		assertThat(count("place_embeds", place)).isEqualTo(1);
	}

	@Test
	void unknownPlaceIs404() {
		assertThat(put(999_999, "sources", "[]")).hasStatus(HttpStatus.NOT_FOUND);
	}

}
