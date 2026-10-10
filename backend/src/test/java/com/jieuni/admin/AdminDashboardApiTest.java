package com.jieuni.admin;

import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

// 지금 = 2026-10-07(수) 10:00 KST.
class AdminDashboardApiTest extends IntegrationTest {

	private static final Instant PUBLISHED = Instant.parse("2026-09-01T00:00:00Z");
	private static final Instant AT = Instant.parse("2026-10-01T00:00:00Z");

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin("admin@test.jieuni");
	}

	@Test
	void picksOfThisAndNextWeekWithEmptySlots() {
		long a = data.place("a", PlaceStatus.PUBLISHED, PUBLISHED);
		long b = data.place("b", PlaceStatus.PUBLISHED, PUBLISHED);
		data.pick(LocalDate.of(2026, 10, 6), a);
		data.pick(LocalDate.of(2026, 10, 13), b);

		var json = assertThat(mvc.get().uri("/api/admin/dashboard").cookie(admin)).hasStatusOk().bodyJson();
		json.extractingPath("$.picks.thisWeek[*].date").asArray().containsExactly("2026-10-05", "2026-10-06",
				"2026-10-07", "2026-10-08", "2026-10-09");
		json.extractingPath("$.picks.nextWeek[*].date").asArray().containsExactly("2026-10-12", "2026-10-13",
				"2026-10-14", "2026-10-15", "2026-10-16");
		json.extractingPath("$.picks.thisWeek[1].place.slug").isEqualTo("a");
		json.extractingPath("$.picks.thisWeek[3].place").isNull();
		json.extractingPath("$.picks.thisWeek[3].locked").isEqualTo(false);
		json.extractingPath("$.picks.thisWeek[2].locked").isEqualTo(true);
		json.extractingPath("$.picks.nextWeek[1].place.slug").isEqualTo("b");
	}

	// 토요일에 보면 이번 주는 끝난 주(10/5~), 다음 주는 곧 열릴 주다.
	@Test
	void weekIsTheKstCalendarWeek() {
		clock.set(Instant.parse("2026-10-10T03:00:00Z"));
		assertThat(mvc.get().uri("/api/admin/dashboard").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.picks.nextWeek[0].date").isEqualTo("2026-10-12");
	}

	@Test
	void countsPendingSuggestionsAndDrafts() {
		long published = data.place("garden", PlaceStatus.PUBLISHED, PUBLISHED);
		data.suggestedPlace("제보 1", null, AT);
		data.suggestedPlace("제보 2", null, AT);
		data.creditSuggestion(published, null, CreditRole.LANDSCAPE, null, AT);
		long declined = data.creditSuggestion(published, null, CreditRole.LIGHTING, null, AT);
		data.jdbc().sql("update credit_suggestions set status = 'declined' where id = :id").param("id", declined).update();
		data.place("d1", PlaceStatus.DRAFT, null);

		var json = assertThat(mvc.get().uri("/api/admin/dashboard").cookie(admin)).hasStatusOk().bodyJson();
		json.extractingPath("$.pendingSuggestions").isEqualTo(3);
		json.extractingPath("$.draftCount").isEqualTo(1);
	}

	// 이번 달은 KST 달력 달이다: 9/30 23:59 KST는 지난달, 10/1 00:30 KST는 이번 달.
	@Test
	void llmCostOfThisKstMonth() {
		assertThat(mvc.get().uri("/api/admin/dashboard").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.llmCostThisMonthUsd").isEqualTo(0);

		data.llmCall("0.500000", Instant.parse("2026-09-30T14:59:00Z"));
		data.llmCall("0.012345", Instant.parse("2026-09-30T15:30:00Z"));
		data.llmCall("0.100000", Instant.parse("2026-10-07T00:00:00Z"));
		data.llmCall("9.000000", Instant.parse("2026-10-31T15:00:00Z"));

		assertThat(mvc.get().uri("/api/admin/dashboard").cookie(admin)).hasStatusOk().bodyJson()
				.extractingPath("$.llmCostThisMonthUsd").isEqualTo(0.112345);
	}

}
