package com.jieuni.admin;

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

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 지금 = 2026-10-07(수) 10:00 KST(IntegrationTest.DEFAULT_NOW).
class AdminPickApiTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni";
	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");
	private static final LocalDate THU = LocalDate.of(2026, 10, 8);
	private static final LocalDate FRI = LocalDate.of(2026, 10, 9);

	@Autowired
	EntityManager em;

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	private MvcTestResult put(LocalDate date, long placeId, String line) {
		return mvc.put().uri("/api/admin/picks/" + date).cookie(admin).with(xsrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"placeId\":%d,\"line\":%s}".formatted(placeId, line == null ? "null" : "\"" + line + "\"")).exchange();
	}

	private MvcTestResult delete(LocalDate date) {
		return mvc.delete().uri("/api/admin/picks/" + date).cookie(admin).with(xsrf()).exchange();
	}

	private String pickedPlace(LocalDate date) {
		return data.jdbc().sql("select p.slug from daily_picks d join places p on p.id = d.place_id where d.pick_date = :d")
				.param("d", date).query(String.class).optional().orElse(null);
	}

	@Test
	void putPicksAPublishedPlace() {
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		data.photo(place, 0);

		var json = assertThat(put(THU, place, " 빛이 오래 머무는 정원 ")).hasStatusOk().bodyJson();
		json.extractingPath("$.date").isEqualTo("2026-10-08");
		json.extractingPath("$.weekday").isEqualTo("THU");
		json.extractingPath("$.line").isEqualTo("빛이 오래 머무는 정원");
		json.extractingPath("$.locked").isEqualTo(false);
		json.extractingPath("$.place.slug").isEqualTo("garden");
		json.extractingPath("$.place.photo.url.w800").isEqualTo("https://photos.test/places/" + place + "/p0-800.webp");
		em.flush();
		assertThat(pickedPlace(THU)).isEqualTo("garden");
	}

	@Test
	void putReplacesTheSameDay() {
		long a = data.place("a", PlaceStatus.PUBLISHED, PUBLISHED);
		long b = data.place("b", PlaceStatus.PUBLISHED, PUBLISHED);
		data.pick(THU, a);
		em.clear();

		assertThat(put(THU, a, "한 줄만 바꾼다")).hasStatusOk().bodyJson().extractingPath("$.line").isEqualTo("한 줄만 바꾼다");
		assertThat(put(THU, b, "다른 공간으로")).hasStatusOk().bodyJson().extractingPath("$.place.slug").isEqualTo("b");
		em.flush();
		assertThat(pickedPlace(THU)).isEqualTo("b");
	}

	@Test
	void weekendIs400() {
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		LocalDate sat = LocalDate.of(2026, 10, 10);
		assertThat(put(sat, place, "주말")).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.message").isEqualTo("월~금만 편성할 수 있어요");
		assertThat(delete(sat)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void onlyPublishedPlaces() {
		for (PlaceStatus status : new PlaceStatus[] {PlaceStatus.DRAFT, PlaceStatus.PAUSED, PlaceStatus.CLOSED}) {
			long place = data.place("p-" + status.value(), status, status == PlaceStatus.DRAFT ? null : PUBLISHED);
			assertThat(put(THU, place, "안 된다")).hasStatus(HttpStatus.CONFLICT).bodyJson()
					.extractingPath("$.message").isEqualTo("공개된 공간만 편성할 수 있어요");
		}
		assertThat(put(THU, 999999, "없는 공간")).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(pickedPlace(THU)).isNull();
	}

	@Test
	void samePlaceOnAnotherDayIs409() {
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		data.pick(THU, place);
		// 제약 위반 뒤에는 테스트 트랜잭션이 끝나 DB를 다시 읽을 수 없다. 응답으로만 본다.
		assertThat(put(FRI, place, "또")).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.extractingPath("$.message").isEqualTo("이미 다른 날에 편성된 공간이에요");
	}

	// 목 07:59 KST에는 목요일 칸을 고칠 수 있고, 08:00 정각부터 열린 칸이라 막힌다.
	@Test
	void todayIsEditableOnlyBefore0800() {
		long a = data.place("a", PlaceStatus.PUBLISHED, PUBLISHED);
		long b = data.place("b", PlaceStatus.PUBLISHED, PUBLISHED);

		clock.set(Instant.parse("2026-10-07T22:59:00Z"));
		assertThat(put(THU, a, "07:59")).hasStatusOk().bodyJson().extractingPath("$.locked").isEqualTo(false);

		clock.set(Instant.parse("2026-10-07T23:00:00Z"));
		assertThat(put(THU, b, "08:00")).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.extractingPath("$.message").isEqualTo("이미 열린 날은 바꿀 수 없어요");
		assertThat(mvc.get().uri("/api/admin/picks?from=" + THU + "&to=" + THU).cookie(admin)).hasStatusOk()
				.bodyJson().extractingPath("$.items[0].locked").isEqualTo(true);

		clock.set(Instant.parse("2026-10-07T23:01:00Z"));
		assertThat(put(THU, b, "08:01")).hasStatus(HttpStatus.CONFLICT).bodyJson()
				.extractingPath("$.message").isEqualTo("이미 열린 날은 바꿀 수 없어요");
		assertThat(delete(THU)).hasStatus(HttpStatus.CONFLICT);
		em.flush();
		assertThat(pickedPlace(THU)).isEqualTo("a");
	}

	@Test
	void pastDayIs409() {
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		assertThat(put(LocalDate.of(2026, 10, 6), place, "어제")).hasStatus(HttpStatus.CONFLICT);
		assertThat(put(LocalDate.of(2026, 10, 7), place, "오늘 10:00")).hasStatus(HttpStatus.CONFLICT);
	}

	@Test
	void lineIsRequiredUpTo40() {
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		assertThat(put(THU, place, "  ")).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors.line").isEqualTo("한 줄을 입력해 주세요");
		assertThat(put(THU, place, "가".repeat(41))).hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors.line").isEqualTo("40자까지 쓸 수 있어요");
		assertThat(put(THU, place, "가".repeat(40))).hasStatusOk();
		assertThat(mvc.put().uri("/api/admin/picks/2026-13-01").cookie(admin).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"placeId\":1,\"line\":\"x\"}"))
				.hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void deleteRemovesAnOpenDay() {
		long place = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		data.pick(FRI, place);
		assertThat(delete(FRI)).hasStatus(HttpStatus.NO_CONTENT);
		em.flush();
		assertThat(pickedPlace(FRI)).isNull();
		assertThat(delete(FRI)).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void listGivesEveryWeekdayWithLockedFlags() {
		long a = data.place("a", PlaceStatus.PUBLISHED, PUBLISHED);
		long b = data.place("b", PlaceStatus.PAUSED, PUBLISHED);
		data.pick(LocalDate.of(2026, 10, 6), a);
		data.pick(FRI, b);
		em.flush();
		em.clear();

		statistics().clear();
		var json = assertThat(mvc.get().uri("/api/admin/picks?from=2026-10-05&to=2026-10-18").cookie(admin))
				.hasStatusOk().bodyJson();
		// 관리자 권한 확인(사용자 역할) 1 + 편성 목록 1. 공간·사진은 목록 쿼리 하나에 함께 온다.
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
		json.extractingPath("$.items[*].date").asArray().containsExactly("2026-10-05", "2026-10-06", "2026-10-07",
				"2026-10-08", "2026-10-09", "2026-10-12", "2026-10-13", "2026-10-14", "2026-10-15", "2026-10-16");
		json.extractingPath("$.items[*].locked").asArray().containsExactly(true, true, true, false, false, false,
				false, false, false, false);
		json.extractingPath("$.items[1].place.slug").isEqualTo("a");
		// 편성 뒤 휴업한 공간도 칸에 그대로 보인다.
		json.extractingPath("$.items[4].place.slug").isEqualTo("b");
		json.extractingPath("$.items[0].place").isNull();
		json.extractingPath("$.items[0].line").isNull();
	}

	@Test
	void badRanges() {
		assertThat(mvc.get().uri("/api/admin/picks?from=2026-10-09&to=2026-10-05").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/admin/picks?from=2026-10-01&to=2026-12-31").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST);
		assertThat(mvc.get().uri("/api/admin/picks?from=2026-10-01").cookie(admin)).hasStatus(HttpStatus.BAD_REQUEST);
	}

	@Test
	void userIs403() {
		Cookie user = devLogin("a@test.jieuni");
		assertThat(mvc.get().uri("/api/admin/picks?from=2026-10-05&to=2026-10-09").cookie(user)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.put().uri("/api/admin/picks/" + THU).cookie(user).with(xsrf()).contentType(MediaType.APPLICATION_JSON)
				.content("{\"placeId\":1,\"line\":\"x\"}")).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.get().uri("/api/admin/dashboard").cookie(user)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.get().uri("/api/admin/suggestions?type=places").cookie(user)).hasStatus(HttpStatus.FORBIDDEN);
	}

}
