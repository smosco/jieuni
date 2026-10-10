package com.jieuni.admin;

import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 재생성 호출은 커밋 뒤에만 나간다. 테스트 트랜잭션 안에서는 커밋이 없어 보이지 않으므로 요청을 실제로 커밋시키고 데이터를 직접 지운다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class RevalidateCommitTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni";
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	@AfterEach
	void cleanUp() {
		var jdbc = data.jdbc();
		String places = "(select id from places where slug like 'reval-%')";
		jdbc.sql("delete from daily_picks where place_id in " + places).update();
		jdbc.sql("delete from credit_suggestions where place_id in " + places).update();
		jdbc.sql("delete from place_photos where place_id in " + places).update();
		jdbc.sql("delete from place_sources where place_id in " + places).update();
		jdbc.sql("delete from place_credits where place_id in " + places).update();
		jdbc.sql("delete from places where slug like 'reval-%'").update();
		jdbc.sql("delete from makers where slug like 'reval-%'").update();
		jdbc.sql("delete from users where provider_id = :email").param("email", ADMIN).update();
	}

	private MvcTestResult send(String method, String uri, String body) {
		var request = switch (method) {
			case "PATCH" -> mvc.patch();
			case "PUT" -> mvc.put();
			case "POST" -> mvc.post();
			case "DELETE" -> mvc.delete();
			default -> throw new IllegalArgumentException(method);
		};
		var spec = request.uri(uri).cookie(admin).with(xsrf());
		if (body != null) {
			spec.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return spec.exchange();
	}

	// publishablePlace가 채운 공개 필수 값을 그대로 다시 보낸다. summary를 비우면 공개 검증에 걸린다.
	private static String editBody(String slug, String name, String summary) {
		return """
				{"slug":"%s","name":"%s","kind":"garden","region":"서울 종로구","address":"서울 종로구 1",
				 "lat":37.5,"lng":127.0,"access":"ticket","summary":%s,"impression":"감상","visitedByEditorOn":"2026-05"}"""
				.formatted(slug, name, summary == null ? "null" : "\"" + summary + "\"");
	}

	@Test
	void firstPublishSendsPlaceMakersBrowseFeedAndSitemap() {
		long place = data.publishablePlace("reval-new", PlaceStatus.DRAFT, null);

		assertThat(send("POST", "/api/admin/places/" + place + "/status", "{\"to\":\"published\"}")).hasStatusOk();

		assertThat(revalidate.calls())
				.containsExactly(Set.of("place:reval-new", "maker:reval-new-maker", "browse", "feed", "sitemap"));
	}

	@Test
	void editingPublishedPlaceSendsItsPages() {
		long place = data.publishablePlace("reval-edit", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(send("PATCH", "/api/admin/places/" + place, editBody("reval-edit", "새 이름", "한 문장")))
				.hasStatusOk();

		assertThat(revalidate.calls())
				.containsExactly(Set.of("place:reval-edit", "maker:reval-edit-maker", "browse", "feed"));
	}

	@Test
	void rolledBackEditSendsNothing() {
		long place = data.publishablePlace("reval-rollback", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(send("PATCH", "/api/admin/places/" + place, editBody("reval-rollback", "새 이름", null)))
				.hasStatus(HttpStatus.BAD_REQUEST);

		assertThat(revalidate.calls()).isEmpty();
	}

	@Test
	void editingDraftSendsNothing() {
		long place = data.publishablePlace("reval-draft", PlaceStatus.DRAFT, null);

		assertThat(send("PATCH", "/api/admin/places/" + place, editBody("reval-draft", "새 이름", "한 문장")))
				.hasStatusOk();

		assertThat(revalidate.calls()).isEmpty();
	}

	@Test
	void replacingListsSendsPlacePages() {
		long place = data.publishablePlace("reval-sources", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(send("PUT", "/api/admin/places/" + place + "/sources", "[]")).hasStatusOk();

		assertThat(revalidate.calls())
				.containsExactly(Set.of("place:reval-sources", "maker:reval-sources-maker", "browse", "feed"));
	}

	@Test
	void pausingSendsPagesWithoutSitemap() {
		long place = data.publishablePlace("reval-pause", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(send("POST", "/api/admin/places/" + place + "/status", "{\"to\":\"paused\"}")).hasStatusOk();

		assertThat(revalidate.calls())
				.containsExactly(Set.of("place:reval-pause", "maker:reval-pause-maker", "browse", "feed"));
	}

	// 사진 등록은 @Transactional이 아니라 TransactionTemplate 안에서 커밋된다.
	@Test
	void addingPhotoSendsPlacePages() {
		long place = data.publishablePlace("reval-photo", PlaceStatus.PUBLISHED, PUBLISHED);
		String key = "uploads/" + place + "/" + UUID.randomUUID();
		storage.putPhoto(key, DEFAULT_NOW);

		assertThat(send("POST", "/api/admin/places/" + place + "/photos", """
				{"objectKey":"%s","width":1600,"height":1067,"alt":"사진 설명","credit":"에디터","license":"own"}""".formatted(key)))
				.hasStatus(HttpStatus.CREATED);

		assertThat(revalidate.calls())
				.containsExactly(Set.of("place:reval-photo", "maker:reval-photo-maker", "browse", "feed"));
	}

	// 빠진 사람의 프로필 격자에서도 공간이 사라진다.
	@Test
	void replacingCreditsSendsRemovedAndAddedMakers() {
		long place = data.publishablePlace("reval-credits", PlaceStatus.PUBLISHED, PUBLISHED);
		long other = data.maker("reval-other");

		assertThat(send("PUT", "/api/admin/places/" + place + "/credits", """
				[{"makerId":%d,"role":"architecture","verified":true,"sourceUrl":"https://example.com/c"}]"""
				.formatted(other))).hasStatusOk();

		assertThat(revalidate.calls()).containsExactly(
				Set.of("place:reval-credits", "maker:reval-credits-maker", "maker:reval-other", "browse", "feed"));
	}

	@Test
	void acceptingCreditSuggestionSendsPlacePages() {
		long place = data.publishablePlace("reval-accept", PlaceStatus.PUBLISHED, PUBLISHED);
		long maker = data.maker("reval-suggested");
		long suggestion = data.creditSuggestion(place, null, CreditRole.LANDSCAPE, "https://example.com/c", PUBLISHED);

		assertThat(send("POST", "/api/admin/suggestions/credits/" + suggestion + "/accept",
				"{\"makerId\":%d,\"verified\":true}".formatted(maker))).hasStatusOk();

		assertThat(revalidate.calls()).containsExactly(
				Set.of("place:reval-accept", "maker:reval-accept-maker", "maker:reval-suggested", "browse", "feed"));
	}

	// 공개 공간의 크레딧에 있는 사람에게 처음 정한 slug는 그때 잠기고 프로필이 밖에 나간다.
	@Test
	void editingMakerSendsProfileOldSlugAndCreditedPlaces() {
		long place = data.publishablePlace("reval-maker", PlaceStatus.PUBLISHED, PUBLISHED);
		long maker = data.jdbc().sql("select maker_id from place_credits where place_id = :id").param("id", place)
				.query(Long.class).single();

		assertThat(send("PATCH", "/api/admin/makers/" + maker,
				"{\"slug\":\"reval-maker-renamed\",\"name\":\"새 이름\",\"kind\":\"person\"}")).hasStatusOk();

		assertThat(revalidate.calls()).containsExactly(Set.of("maker:reval-maker-renamed", "maker:reval-maker-maker",
				"place:reval-maker", "browse", "feed", "sitemap"));
	}

	@Test
	void editingMakerWithoutPublicPlacesSendsNothing() {
		long maker = data.maker("reval-lonely");

		assertThat(send("PATCH", "/api/admin/makers/" + maker,
				"{\"slug\":\"reval-lonely\",\"name\":\"새 이름\",\"kind\":\"person\"}")).hasStatusOk();

		assertThat(revalidate.calls()).isEmpty();
	}

	// 기본 시각은 수 10:00 KST라 금요일 칸은 아직 고칠 수 있다.
	@Test
	void changingPickSendsFeed() {
		long place = data.publishablePlace("reval-pick", PlaceStatus.PUBLISHED, PUBLISHED);

		assertThat(send("PUT", "/api/admin/picks/2026-10-09",
				"{\"placeId\":%d,\"line\":\"한 줄\"}".formatted(place))).hasStatusOk();
		assertThat(send("DELETE", "/api/admin/picks/2026-10-09", null)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(revalidate.calls()).containsExactly(Set.of("feed"), Set.of("feed"));
	}

}
