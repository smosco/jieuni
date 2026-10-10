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
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPhotoApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni"; // application-test.yml의 ADMIN_EMAILS
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	@Autowired
	EntityManager em;

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	// 서명 URL로 두 크기가 올라간 상태를 만든다.
	private String uploaded(long placeId) {
		String key = "uploads/" + placeId + "/" + UUID.randomUUID();
		storage.putPhoto(key, DEFAULT_NOW);
		return key;
	}

	private MvcTestResult add(long placeId, String body) {
		return mvc.post().uri("/api/admin/places/" + placeId + "/photos").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private static String addBody(String key, String license, String sourceUrl) {
		return """
				{"objectKey":"%s","width":1600,"height":1067,"alt":"정원","credit":"에디터","license":"%s"%s}"""
				.formatted(key, license, sourceUrl == null ? "" : ",\"sourceUrl\":\"" + sourceUrl + "\"");
	}

	private int count(long placeId) {
		return data.jdbc().sql("select count(*) from place_photos where place_id = :id").param("id", placeId)
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
		assertThat(mvc.post().uri("/api/admin/places/" + place + "/photos").cookie(user).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(addBody(uploaded(place), "own", null)))
				.hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.delete().uri("/api/admin/places/" + place + "/photos/1").cookie(user).with(xsrf()))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

	@Test
	void addedPhotoGoesLastWithPublicUrls() {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		data.photo(place, 0);
		String key = uploaded(place);

		var json = assertThat(add(place, addBody(key, "press", "https://example.com/press")))
				.hasStatus(HttpStatus.CREATED).bodyJson();
		json.extractingPath("$.photos[1].objectKey").isEqualTo(key);
		json.extractingPath("$.photos[1].position").isEqualTo(1);
		json.extractingPath("$.photos[1].license").isEqualTo("press");
		json.extractingPath("$.photos[1].sourceUrl").isEqualTo("https://example.com/press");
		// 공개 URL은 업로드한 파일 이름과 같다.
		json.extractingPath("$.photos[1].url.w1600").isEqualTo("https://photos.test/" + key + "-1600.webp");
		json.extractingPath("$.photos[1].url.w800").isEqualTo("https://photos.test/" + key + "-800.webp");
		assertThat(count(place)).isEqualTo(2);
	}

	@Test
	void photoNotFullyUploadedIs400() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		String none = "uploads/" + place + "/" + UUID.randomUUID();
		String half = "uploads/" + place + "/" + UUID.randomUUID();
		storage.put(half + "-1600.webp", DEFAULT_NOW);

		for (String key : List.of(none, half)) {
			MvcTestResult result = add(place, addBody(key, "own", null));
			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(fieldErrors(result)).containsExactly(Map.entry("objectKey", "업로드가 끝나지 않은 사진이에요"));
		}
		assertThat(count(place)).isZero();
	}

	@Test
	void keyMustBeIssuedForThisPlace() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		long other = data.place("q", PlaceStatus.DRAFT, null);

		for (String key : List.of(uploaded(other), "places/" + place + "/x", "uploads/" + place + "/../x")) {
			MvcTestResult result = add(place, addBody(key, "own", null));
			assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(fieldErrors(result)).containsExactly(Map.entry("objectKey", "이 공간의 업로드 키가 아니에요"));
		}
	}

	@Test
	void notOwnNeedsSourceUrlAndFormatErrorsHaveKeys() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);

		MvcTestResult noSource = add(place, addBody(uploaded(place), "credited", null));
		assertThat(noSource).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(noSource)).containsExactly(Map.entry("sourceUrl", "출처 링크를 입력해 주세요"));

		MvcTestResult format = add(place, """
				{"objectKey":"","width":0,"alt":"","license":null,"sourceUrl":"ftp://x"}""");
		assertThat(format).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(format)).containsOnlyKeys("objectKey", "width", "height", "license", "sourceUrl");
		assertThat(count(place)).isZero();
	}

	@Test
	void sameKeyTwiceIs409() {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		String key = uploaded(place);
		assertThat(add(place, addBody(key, "own", null))).hasStatus(HttpStatus.CREATED);
		assertThat(add(place, addBody(key, "own", null))).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.extractingPath("$.message").isEqualTo("이미 등록된 사진이에요");
	}

	@Test
	void editReplacesAllFourFields() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		long photo = data.photo(place, 0, "uploads/" + place + "/a");
		String uri = "/api/admin/places/" + place + "/photos/" + photo;

		var json = assertThat(mvc.patch().uri(uri).cookie(admin).with(xsrf()).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"alt":" 빛 ","license":"credited","sourceUrl":"https://example.com/c"}""")).hasStatusOk().bodyJson();
		json.extractingPath("$.photos[0].alt").isEqualTo("빛");
		json.extractingPath("$.photos[0].credit").isNull();
		json.extractingPath("$.photos[0].license").isEqualTo("credited");

		MvcTestResult noSource = mvc.patch().uri(uri).cookie(admin).with(xsrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"license\":\"press\"}").exchange();
		assertThat(noSource).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(noSource)).containsExactly(Map.entry("sourceUrl", "출처 링크를 입력해 주세요"));

		assertThat(mvc.patch().uri("/api/admin/places/" + place + "/photos/999999").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"license\":\"own\"}")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void reorderSetsPositionsAndCover() throws UnsupportedEncodingException {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		long a = data.photo(place, 0, "uploads/" + place + "/a");
		long b = data.photo(place, 1, "uploads/" + place + "/b");
		long c = data.photo(place, 2, "uploads/" + place + "/c");
		String uri = "/api/admin/places/" + place + "/photos/order";

		var json = assertThat(mvc.put().uri(uri).cookie(admin).with(xsrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"photoIds\":[%d,%d,%d]}".formatted(c, a, b))).hasStatusOk().bodyJson();
		json.extractingPath("$.photos[*].id").asArray().containsExactly((int) c, (int) a, (int) b);
		json.extractingPath("$.photos[*].position").asArray().containsExactly(0, 1, 2);
		assertThat(data.jdbc().sql("select object_key from place_photos where place_id = :id and position = 0")
				.param("id", place).query(String.class).single()).isEqualTo("uploads/" + place + "/c");

		for (String ids : List.of("[%d,%d]".formatted(a, b), "[%d,%d,%d]".formatted(a, a, b),
				"[%d,%d,999999]".formatted(a, b))) {
			MvcTestResult bad = mvc.put().uri(uri).cookie(admin).with(xsrf()).contentType(MediaType.APPLICATION_JSON)
					.content("{\"photoIds\":" + ids + "}").exchange();
			assertThat(bad).hasStatus(HttpStatus.BAD_REQUEST);
			assertThat(fieldErrors(bad)).containsExactly(Map.entry("photoIds", "사진 목록이 맞지 않아요"));
		}
	}

	@Test
	void deleteRenumbersRemainingPhotos() {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		long a = data.photo(place, 0, "uploads/" + place + "/a");
		data.photo(place, 1, "uploads/" + place + "/b");
		data.photo(place, 2, "uploads/" + place + "/c");

		assertThat(mvc.delete().uri("/api/admin/places/" + place + "/photos/" + a).cookie(admin).with(xsrf()))
				.hasStatus(HttpStatus.NO_CONTENT);
		em.clear();
		assertThat(mvc.get().uri("/api/admin/places/" + place).cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.photos[*].position").asArray().containsExactly(0, 1);
		assertThat(data.jdbc().sql("select object_key from place_photos where place_id = :id and position = 0")
				.param("id", place).query(String.class).single()).isEqualTo("uploads/" + place + "/b");
		assertThat(mvc.delete().uri("/api/admin/places/" + place + "/photos/" + a).cookie(admin).with(xsrf()))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void deletingLastPhotoOfPublishedPlaceIs400() throws UnsupportedEncodingException {
		long place = data.publishablePlace("p", PlaceStatus.PUBLISHED, PUBLISHED);
		long photo = data.jdbc().sql("select id from place_photos where place_id = :id").param("id", place)
				.query(Long.class).single();

		MvcTestResult result = mvc.delete().uri("/api/admin/places/" + place + "/photos/" + photo).cookie(admin)
				.with(xsrf()).exchange();
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.message").isEqualTo("공개된 공간이라 이 값은 비울 수 없어요");
		assertThat(fieldErrors(result)).containsExactly(Map.entry("photos", "사진 또는 임베드가 1개 이상 필요해요"));
		em.clear();
		assertThat(count(place)).isEqualTo(1);
	}

	@Test
	void editingPublishedPhotoMustStayPublishable() throws UnsupportedEncodingException {
		long place = data.publishablePlace("p", PlaceStatus.PUBLISHED, PUBLISHED);
		long photo = data.jdbc().sql("select id from place_photos where place_id = :id").param("id", place)
				.query(Long.class).single();

		MvcTestResult result = mvc.patch().uri("/api/admin/places/" + place + "/photos/" + photo).cookie(admin)
				.with(xsrf()).contentType(MediaType.APPLICATION_JSON).content("{\"license\":\"own\"}").exchange();
		assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(fieldErrors(result)).containsOnlyKeys("photos[0].alt", "photos[0].credit");
	}

}
