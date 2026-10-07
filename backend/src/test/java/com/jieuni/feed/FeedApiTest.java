package com.jieuni.feed;

import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

// 피드 공개 경계: 수 07:59 / 08:00, 금 08:00 뒤 다음 공개 시각, 토, 월 07:59 지난주, 편성 빈 날.
// nextRelative(마지막 화면 문구): 평일 08:00 전 today, 다음 공개일이 내일이면 tomorrow(일요일 포함), 그 밖 later.
class FeedApiTest extends IntegrationTest {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final LocalDate LAST_MONDAY = LocalDate.of(2026, 9, 28);
	private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

	@BeforeEach
	void scheduleTwoWeeks() {
		// 지난주 월~금 5칸, 이번 주 월·화·수·금(목은 비움).
		for (int i = 0; i < 5; i++) {
			data.pick(LAST_MONDAY.plusDays(i), place("last-" + i));
		}
		for (int i : new int[] {0, 1, 2, 4}) {
			data.pick(MONDAY.plusDays(i), place("this-" + i));
		}
	}

	private long place(String slug) {
		return data.place(slug, PlaceStatus.PUBLISHED, Instant.parse("2026-09-01T00:00:00Z"));
	}

	private void nowKst(String localDateTime) {
		clock.set(LocalDateTime.parse(localDateTime).atZone(KST).toInstant());
	}

	@Test
	void wednesday0759ShowsMondayAndTuesday() {
		nowKst("2026-10-07T07:59");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.picks[*].place.slug").asArray().containsExactly("this-1", "this-0");
		json.extractingPath("$.picks[*].weekday").asArray().containsExactly("TUE", "MON");
		json.extractingPath("$.openedCount").isEqualTo(2);
		json.extractingPath("$.weekLabel").isEqualTo("2026-10-05");
		json.extractingPath("$.nextOpensAt").isEqualTo("2026-10-06T23:00:00Z"); // 수 08:00 KST
		json.extractingPath("$.nextWeekday").isEqualTo("WED");
		json.extractingPath("$.nextRelative").isEqualTo("today");
	}

	@Test
	void wednesday0800AddsWednesdayOnTop() {
		nowKst("2026-10-07T08:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.picks[*].place.slug").asArray().containsExactly("this-2", "this-1", "this-0");
		json.extractingPath("$.openedCount").isEqualTo(3);
		json.extractingPath("$.nextOpensAt").isEqualTo("2026-10-07T23:00:00Z"); // 목 08:00 KST
		json.extractingPath("$.nextWeekday").isEqualTo("THU");
		json.extractingPath("$.nextRelative").isEqualTo("tomorrow");
	}

	@Test
	void emptyThursdayIsSkipped() {
		nowKst("2026-10-08T12:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.picks[*].place.slug").asArray().containsExactly("this-2", "this-1", "this-0");
		json.extractingPath("$.nextWeekday").isEqualTo("FRI");
		json.extractingPath("$.nextRelative").isEqualTo("tomorrow");
	}

	@Test
	void afterFriday0800NextOpeningIsMonday() {
		nowKst("2026-10-09T08:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.picks[*].place.slug").asArray().containsExactly("this-4", "this-2", "this-1", "this-0");
		json.extractingPath("$.nextOpensAt").isEqualTo("2026-10-11T23:00:00Z"); // 다음 월 08:00 KST
		json.extractingPath("$.nextWeekday").isEqualTo("MON");
		json.extractingPath("$.nextRelative").isEqualTo("later");
	}

	@Test
	void saturdayShowsTheWholeWeek() {
		nowKst("2026-10-10T15:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.openedCount").isEqualTo(4);
		json.extractingPath("$.weekLabel").isEqualTo("2026-10-05");
		json.extractingPath("$.nextOpensAt").isEqualTo("2026-10-11T23:00:00Z");
		json.extractingPath("$.nextRelative").isEqualTo("later");
	}

	@Test
	void sundayNextOpeningIsTomorrow() {
		nowKst("2026-10-11T21:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.weekLabel").isEqualTo("2026-10-05");
		json.extractingPath("$.nextOpensAt").isEqualTo("2026-10-11T23:00:00Z");
		json.extractingPath("$.nextWeekday").isEqualTo("MON");
		json.extractingPath("$.nextRelative").isEqualTo("tomorrow");
	}

	@Test
	void monday0759StillShowsLastWeek() {
		nowKst("2026-10-05T07:59");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.weekLabel").isEqualTo("2026-09-28");
		json.extractingPath("$.picks[*].place.slug").asArray()
				.containsExactly("last-4", "last-3", "last-2", "last-1", "last-0");
		json.extractingPath("$.openedCount").isEqualTo(5);
		json.extractingPath("$.nextOpensAt").isEqualTo("2026-10-04T23:00:00Z"); // 오늘(월) 08:00 KST
		json.extractingPath("$.nextWeekday").isEqualTo("MON");
		json.extractingPath("$.nextRelative").isEqualTo("today");
	}

	@Test
	void monday0800SwitchesToTheNewWeek() {
		nowKst("2026-10-05T08:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.weekLabel").isEqualTo("2026-10-05");
		json.extractingPath("$.picks[*].place.slug").asArray().containsExactly("this-0");
		json.extractingPath("$.nextRelative").isEqualTo("tomorrow");
	}

	@Test
	void pausedAndClosedPicksStayWithStatusAndDraftIsDropped() {
		setStatus("this-0", "paused");
		setStatus("this-1", "closed");
		setStatus("this-2", "draft");
		nowKst("2026-10-10T15:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.picks[*].place.slug").asArray().containsExactly("this-4", "this-1", "this-0");
		json.extractingPath("$.picks[*].place.status").asArray().containsExactly("published", "closed", "paused");
		json.extractingPath("$.openedCount").isEqualTo(3);
	}

	private void setStatus(String slug, String status) {
		data.jdbc().sql("update places set status = :status where slug = :slug")
				.param("status", status).param("slug", slug).update();
	}

	@Test
	void pickCarriesTileAndFirstMakerByPosition() {
		long place = data.jdbc().sql("select place_id from daily_picks where pick_date = '2026-10-05'")
				.query(Long.class).single();
		data.credit(place, data.maker("second"), CreditRole.LIGHTING, 1);
		data.credit(place, data.maker("first"), CreditRole.ARCHITECTURE, 0);
		data.photo(place, 0);
		nowKst("2026-10-05T09:00");

		var json = assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson();
		json.extractingPath("$.picks[0].date").isEqualTo("2026-10-05");
		json.extractingPath("$.picks[0].line").isEqualTo("2026-10-05의 한 줄");
		json.extractingPath("$.picks[0].firstMaker.slug").isEqualTo("first");
		json.extractingPath("$.picks[0].firstMaker.role").isEqualTo("architecture");
		json.extractingPath("$.picks[0].place.caption").isNull();
		json.extractingPath("$.picks[0].place.photo.url.w1600")
				.isEqualTo("https://photos.test/places/" + place + "/p0-1600.webp");
	}

	@Test
	void pickWithoutCreditsHasNullFirstMaker() {
		nowKst("2026-10-05T09:00");

		assertThat(mvc.get().uri("/api/feed")).hasStatusOk().bodyJson()
				.extractingPath("$.picks[0].firstMaker").isNull();
	}

	@Test
	void feedRunsTwoQueries() {
		nowKst("2026-10-10T15:00");

		statistics().clear();
		assertThat(mvc.get().uri("/api/feed")).hasStatusOk();
		// 편성+공간+대표 사진 1 · 첫 크레딧 1
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

}
