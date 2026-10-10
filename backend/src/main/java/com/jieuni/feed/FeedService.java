package com.jieuni.feed;

import com.jieuni.place.PhotoUrls;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class FeedService {

	private final DailyPickRepository picks;
	private final PhotoUrls photoUrls;
	private final Clock clock;

	FeedService(DailyPickRepository picks, PhotoUrls photoUrls, Clock clock) {
		this.picks = picks;
		this.photoUrls = photoUrls;
		this.clock = clock;
	}

	public FeedResponse feed() {
		// 경계 계산만 KST로 한다. 저장·응답 시각은 UTC.
		Instant now = clock.instant();
		LocalDate today = now.atZone(PickTime.KST).toLocalDate();
		LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		// 월요일 08:00 전에는 새 주가 아직 열리지 않았으므로 지난주를 보여 준다.
		LocalDate weekMonday = PickTime.opened(thisMonday, now) ? thisMonday : thisMonday.minusWeeks(1);
		// 열린 칸은 오늘 08:00이 지났으면 오늘까지, 아니면 어제까지다.
		LocalDate lastOpened = PickTime.opened(today, now) ? today : today.minusDays(1);
		LocalDate to = lastOpened.isBefore(weekMonday.plusDays(4)) ? lastOpened : weekMonday.plusDays(4);

		List<FeedPickRow> rows = picks.findOpened(weekMonday, to, PlaceStatus.HAS_PAGE);

		Map<Long, FirstMakerRow> firstMakers = new HashMap<>();
		if (!rows.isEmpty()) {
			List<Long> placeIds = rows.stream().map(row -> row.place().id()).toList();
			for (FirstMakerRow credit : picks.findCreditsInOrder(placeIds)) {
				firstMakers.putIfAbsent(credit.placeId(), credit);
			}
		}

		List<FeedResponse.Pick> items = rows.stream().map(row -> {
			FirstMakerRow maker = firstMakers.get(row.place().id());
			return new FeedResponse.Pick(row.date(), PickTime.weekdayName(row.date()), row.line(),
					PlaceTile.of(row.place(), photoUrls.of(row.place().photo()), null),
					maker == null ? null
							: new FeedResponse.FirstMaker(maker.id(), maker.slug(), maker.name(), maker.role()));
		}).toList();

		LocalDate next = nextOpening(today, now);
		return new FeedResponse(weekMonday, items, items.size(), PickTime.opensAt(next), PickTime.weekdayName(next),
				relative(today, next));
	}

	// 평일 08:00 전이면 오늘, 다음 공개일이 내일이면(일요일 포함) 내일, 금 08:00 뒤·토요일은 그 밖.
	private static FeedResponse.NextRelative relative(LocalDate today, LocalDate nextDate) {
		if (nextDate.equals(today)) {
			return FeedResponse.NextRelative.TODAY;
		}
		return nextDate.equals(today.plusDays(1)) ? FeedResponse.NextRelative.TOMORROW
				: FeedResponse.NextRelative.LATER;
	}

	// 아직 열리지 않은 가장 가까운 평일. 편성이 없는 날이어도 준다(마지막 화면이 "언제 열리는지"를 늘 말할 수 있게).
	private static LocalDate nextOpening(LocalDate today, Instant now) {
		LocalDate date = today;
		while (!PickTime.weekday(date) || PickTime.opened(date, now)) {
			date = date.plusDays(1);
		}
		return date;
	}

}
