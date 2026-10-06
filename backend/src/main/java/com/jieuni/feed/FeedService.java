package com.jieuni.feed;

import com.jieuni.place.PhotoUrls;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class FeedService {

	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final LocalTime OPENS_AT = LocalTime.of(8, 0);

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
		ZonedDateTime now = clock.instant().atZone(KST);
		LocalDate today = now.toLocalDate();
		LocalDate thisMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		// 월요일 08:00 전에는 새 주가 아직 열리지 않았으므로 지난주를 보여 준다.
		LocalDate weekMonday = now.isBefore(thisMonday.atTime(OPENS_AT).atZone(KST)) ? thisMonday.minusWeeks(1)
				: thisMonday;
		// pick_date 08:00 KST <= 지금 ⇔ pick_date <= 마지막으로 열린 날짜
		LocalDate lastOpened = now.toLocalTime().isBefore(OPENS_AT) ? today.minusDays(1) : today;
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
			return new FeedResponse.Pick(row.date(), weekday(row.date().getDayOfWeek()), row.line(),
					PlaceTile.of(row.place(), photoUrls.of(row.place().photo()), null),
					maker == null ? null
							: new FeedResponse.FirstMaker(maker.id(), maker.slug(), maker.name(), maker.role()));
		}).toList();

		ZonedDateTime next = nextOpening(now);
		return new FeedResponse(weekMonday, items, items.size(), next.toInstant(), weekday(next.getDayOfWeek()),
				relative(today, next.toLocalDate()));
	}

	// 평일 08:00 전이면 오늘, 다음 공개일이 내일이면(일요일 포함) 내일, 금 08:00 뒤·토요일은 그 밖.
	private static FeedResponse.NextRelative relative(LocalDate today, LocalDate nextDate) {
		if (nextDate.equals(today)) {
			return FeedResponse.NextRelative.TODAY;
		}
		return nextDate.equals(today.plusDays(1)) ? FeedResponse.NextRelative.TOMORROW
				: FeedResponse.NextRelative.LATER;
	}

	// 지금보다 뒤인 가장 가까운 평일 08:00 KST. 편성이 없는 날이어도 시각은 준다(API.md 1절).
	private static ZonedDateTime nextOpening(ZonedDateTime now) {
		ZonedDateTime candidate = now.toLocalDate().atTime(OPENS_AT).atZone(KST);
		while (!candidate.isAfter(now) || candidate.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue()) {
			candidate = candidate.plusDays(1);
		}
		return candidate;
	}

	private static String weekday(DayOfWeek day) {
		return day.name().substring(0, 3);
	}

}
