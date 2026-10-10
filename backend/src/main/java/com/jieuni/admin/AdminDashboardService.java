package com.jieuni.admin;

import com.jieuni.ai.LlmCallRepository;
import com.jieuni.feed.PickTime;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceStatus;
import com.jieuni.suggestion.CreditSuggestionRepository;
import com.jieuni.suggestion.SuggestionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
public class AdminDashboardService {

	private final AdminPickService adminPickService;
	private final PlaceRepository places;
	private final CreditSuggestionRepository credits;
	private final LlmCallRepository llmCalls;
	private final Clock clock;

	AdminDashboardService(AdminPickService adminPickService, PlaceRepository places,
			CreditSuggestionRepository credits, LlmCallRepository llmCalls, Clock clock) {
		this.adminPickService = adminPickService;
		this.places = places;
		this.credits = credits;
		this.llmCalls = llmCalls;
		this.clock = clock;
	}

	public record Dashboard(Picks picks, long pendingSuggestions, long draftCount, BigDecimal llmCostThisMonthUsd) {
	}

	public record Picks(List<AdminPick> thisWeek, List<AdminPick> nextWeek) {
	}

	// 이번 주는 KST 달력 주(월~일)다. 주말에 보면 끝난 이번 주와 곧 열 다음 주가 함께 보인다.
	@Transactional(readOnly = true)
	public Dashboard get() {
		LocalDate today = clock.instant().atZone(PickTime.KST).toLocalDate();
		LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		List<AdminPick> twoWeeks = adminPickService.list(monday, monday.plusDays(11));
		LocalDate firstOfMonth = today.withDayOfMonth(1);
		BigDecimal cost = llmCalls.sumCost(firstOfMonth.atStartOfDay(PickTime.KST).toInstant(),
				firstOfMonth.plusMonths(1).atStartOfDay(PickTime.KST).toInstant());
		return new Dashboard(new Picks(twoWeeks.subList(0, 5), twoWeeks.subList(5, 10)),
				places.countByStatus(PlaceStatus.SUGGESTED) + credits.countByStatus(SuggestionStatus.PENDING),
				places.countByStatus(PlaceStatus.DRAFT), cost);
	}

}
