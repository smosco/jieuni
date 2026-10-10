package com.jieuni.admin;

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

import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AdminSuggestionApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni";
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");
	private static final Instant EARLIER = Instant.parse("2026-10-01T00:00:00Z");
	private static final Instant LATER = Instant.parse("2026-10-02T00:00:00Z");

	@Autowired
	EntityManager em;

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	private MvcTestResult post(String path, String body) {
		var request = mvc.post().uri("/api/admin/suggestions" + path).cookie(admin).with(xsrf());
		if (body != null) {
			request.contentType(MediaType.APPLICATION_JSON).content(body);
		}
		return request.exchange();
	}

	private String placeStatus(long id) {
		return data.jdbc().sql("select status from places where id = :id").param("id", id).query(String.class).single();
	}

	private String suggestionStatus(long id) {
		return data.jdbc().sql("select status from credit_suggestions where id = :id").param("id", id).query(String.class).single();
	}

	@Test
	void placeSuggestionsOldestFirst() {
		devLogin("me@test.jieuni");
		long me = data.userId("me@test.jieuni");
		long later = data.suggestedPlace("나중 제보", me, LATER);
		long earlier = data.suggestedPlace("탈퇴한 사람의 제보", null, EARLIER);
		data.place("draft-place", PlaceStatus.DRAFT, null);

		var json = assertThat(mvc.get().uri("/api/admin/suggestions?type=places").cookie(admin)).hasStatusOk().bodyJson();
		json.extractingPath("$.items[*].id").asArray().containsExactly((int) earlier, (int) later);
		json.extractingPath("$.items[0].suggestedBy").isNull();
		json.extractingPath("$.items[1].suggestedBy.nickname").isNotNull();
		json.extractingPath("$.items[1].reason").isEqualTo("오래 생각나요");
		json.extractingPath("$.items[1].link").isEqualTo("https://example.com/s");
		json.extractingPath("$.items[1].makerHint").isEqualTo("건축가 누구");

		assertThat(post("/places/" + earlier + "/reject", null)).hasStatus(HttpStatus.NO_CONTENT);
		assertThat(mvc.get().uri("/api/admin/suggestions?type=places&status=rejected").cookie(admin)).hasStatusOk()
				.bodyJson().extractingPath("$.items[*].id").asArray().containsExactly((int) earlier);
	}

	@Test
	void startMakesDraftOnlyFromSuggested() {
		long place = data.suggestedPlace("숨은 정원", null, EARLIER);

		assertThat(post("/places/" + place + "/start", null)).hasStatusOk().bodyJson()
				.extractingPath("$.status").isEqualTo("draft");
		em.flush();
		assertThat(placeStatus(place)).isEqualTo("draft");
		assertThat(post("/places/" + place + "/start", null)).hasStatus(HttpStatus.CONFLICT);
		assertThat(post("/places/" + place + "/reject", null)).hasStatus(HttpStatus.CONFLICT);
		assertThat(post("/places/999999/start", null)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void creditSuggestionsList() {
		devLogin("me@test.jieuni");
		long me = data.userId("me@test.jieuni");
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		long first = data.creditSuggestion(place, me, CreditRole.LANDSCAPE, "https://example.com/c", EARLIER);
		data.creditSuggestion(place, null, CreditRole.LIGHTING, null, LATER);

		var json = assertThat(mvc.get().uri("/api/admin/suggestions?type=credits").cookie(admin)).hasStatusOk().bodyJson();
		json.extractingPath("$.items.length()").isEqualTo(2);
		json.extractingPath("$.items[0].id").isEqualTo((int) first);
		json.extractingPath("$.items[0].place.slug").isEqualTo("garden");
		json.extractingPath("$.items[0].role").isEqualTo("landscape");
		json.extractingPath("$.items[0].makerName").isEqualTo("제보된 이름");
		json.extractingPath("$.items[0].status").isEqualTo("pending");
		json.extractingPath("$.items[0].user.id").isEqualTo((int) me);
		json.extractingPath("$.items[1].user").isNull();
		assertThat(mvc.get().uri("/api/admin/suggestions?type=credits&status=accepted").cookie(admin)).hasStatusOk()
				.bodyJson().extractingPath("$.items").asArray().isEmpty();
	}

	@Test
	void unknownTypeOrStatusIs400() {
		assertThat(mvc.get().uri("/api/admin/suggestions").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/admin/suggestions?type=makers").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/admin/suggestions?type=places&status=draft").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/admin/suggestions?type=credits&status=done").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void acceptAppendsCreditAndLocksMakerSlug() {
		long place = data.publishablePlace("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		long maker = data.maker("new-maker");
		long suggestion = data.creditSuggestion(place, null, CreditRole.LANDSCAPE, "https://example.com/c", EARLIER);

		var json = assertThat(post("/credits/" + suggestion + "/accept", "{\"makerId\":%d,\"verified\":true}".formatted(maker)))
				.hasStatusOk().bodyJson();
		json.extractingPath("$.credits.length()").isEqualTo(2);
		json.extractingPath("$.credits[1].makerId").isEqualTo((int) maker);
		json.extractingPath("$.credits[1].role").isEqualTo("landscape");
		json.extractingPath("$.credits[1].verified").isEqualTo(true);
		// 출처를 보내지 않으면 제보에 적힌 출처를 쓴다.
		json.extractingPath("$.credits[1].sourceUrl").isEqualTo("https://example.com/c");

		em.flush();
		var row = data.jdbc().sql("select status, reviewed_at from credit_suggestions where id = :id").param("id", suggestion)
				.query().singleRow();
		assertThat(row.get("status")).isEqualTo("accepted");
		assertThat(((Timestamp) row.get("reviewed_at")).toInstant()).isEqualTo(DEFAULT_NOW);
		assertThat(data.jdbc().sql("select slug_locked_at is not null from makers where id = :id").param("id", maker)
				.query(Boolean.class).single()).isTrue();

		assertThat(post("/credits/" + suggestion + "/accept", "{\"makerId\":%d,\"verified\":false}".formatted(maker)))
				.hasStatus(HttpStatus.CONFLICT).bodyJson().extractingPath("$.message").isEqualTo("이미 검토한 제보예요");
	}

	@Test
	void acceptErrorsUseRequestKeys() {
		long place = data.publishablePlace("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		long maker = data.maker("new-maker");
		long suggestion = data.creditSuggestion(place, null, CreditRole.LANDSCAPE, null, EARLIER);

		assertThat(post("/credits/" + suggestion + "/accept", "{\"makerId\":%d,\"verified\":true}".formatted(maker)))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors.sourceUrl").isEqualTo("확인됨이면 출처 링크가 필요해요");
		assertThat(post("/credits/" + suggestion + "/accept", "{\"makerId\":999999,\"verified\":false}"))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors.makerId").isEqualTo("없는 만든 사람이에요");
		assertThat(post("/credits/" + suggestion + "/accept", "{\"verified\":false}")).hasStatus(HttpStatus.BAD_REQUEST);
		em.clear();
		assertThat(suggestionStatus(suggestion)).isEqualTo("pending");
		assertThat(data.jdbc().sql("select count(*) from place_credits where place_id = :id").param("id", place)
				.query(Integer.class).single()).isEqualTo(1);
		assertThat(post("/credits/999999/accept", "{\"makerId\":%d,\"verified\":false}".formatted(maker)))
				.hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void declineOnlyPending() {
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		long suggestion = data.creditSuggestion(place, null, CreditRole.LANDSCAPE, null, EARLIER);

		assertThat(post("/credits/" + suggestion + "/decline", null)).hasStatus(HttpStatus.NO_CONTENT);
		em.flush();
		assertThat(suggestionStatus(suggestion)).isEqualTo("declined");
		assertThat(post("/credits/" + suggestion + "/decline", null)).hasStatus(HttpStatus.CONFLICT);
	}

}
