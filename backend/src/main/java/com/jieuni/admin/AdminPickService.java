package com.jieuni.admin;

import com.jieuni.common.ApiException;
import com.jieuni.common.PublicPagesChanged;
import com.jieuni.feed.AdminPickRow;
import com.jieuni.feed.DailyPick;
import com.jieuni.feed.DailyPickRepository;
import com.jieuni.feed.PickTime;
import com.jieuni.place.PhotoUrls;
import com.jieuni.place.Place;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AdminPickService {

	// 편성 화면은 두 주씩 본다. 한 번에 너무 넓은 범위를 받지 않게 막는다.
	private static final int MAX_RANGE_DAYS = 62;

	private final DailyPickRepository picks;
	private final PlaceRepository places;
	private final PhotoUrls photoUrls;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	AdminPickService(DailyPickRepository picks, PlaceRepository places, PhotoUrls photoUrls,
			ApplicationEventPublisher events, Clock clock) {
		this.picks = picks;
		this.places = places;
		this.photoUrls = photoUrls;
		this.events = events;
		this.clock = clock;
	}

	// 범위 안의 평일 전부. 편성이 없는 날도 빈 칸으로 준다(화면이 빈 칸을 강조한다).
	@Transactional(readOnly = true)
	public List<AdminPick> list(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "시작 날짜가 끝 날짜보다 늦어요");
		}
		if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "한 번에 " + MAX_RANGE_DAYS + "일까지 볼 수 있어요");
		}
		Map<LocalDate, AdminPickRow> rows = picks.findAdminRows(from, to).stream()
				.collect(Collectors.toMap(AdminPickRow::date, Function.identity()));
		Instant now = clock.instant();
		List<AdminPick> days = new ArrayList<>();
		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			if (PickTime.weekday(date)) {
				days.add(toPick(date, rows.get(date), now));
			}
		}
		return days;
	}

	@Transactional
	public AdminPick put(LocalDate date, long placeId, String line) {
		requireEditable(date);
		Place place = places.findById(placeId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공간을 찾을 수 없어요"));
		if (place.getStatus() != PlaceStatus.PUBLISHED) {
			throw new ApiException(HttpStatus.CONFLICT, "공개된 공간만 편성할 수 있어요");
		}
		picks.findById(date).ifPresentOrElse(pick -> pick.change(place, line),
				() -> picks.save(new DailyPick(date, place, line)));
		// 한 공간은 한 번만 편성한다(uq_daily_picks_place). 응답 전에 내보내 409로 바꾼다.
		picks.flush();
		feedChanged();
		return toPick(date, picks.findAdminRows(date, date).getFirst(), clock.instant());
	}

	@Transactional
	public void delete(LocalDate date) {
		requireEditable(date);
		DailyPick pick = picks.findById(date)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "이 날에는 편성이 없어요"));
		picks.delete(pick);
		feedChanged();
	}

	// 열린 칸은 못 고치므로 지금 피드는 그대로지만, 편성이 바뀌면 피드를 비운다는 규칙 하나로 둔다.
	private void feedChanged() {
		events.publishEvent(PublicPagesChanged.feed());
	}

	// 주말 칸은 없다. 이미 열린 칸(피드에 보인 칸)은 바꾸지 않는다.
	private void requireEditable(LocalDate date) {
		if (!PickTime.weekday(date)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "월~금만 편성할 수 있어요");
		}
		if (PickTime.opened(date, clock.instant())) {
			throw new ApiException(HttpStatus.CONFLICT, "이미 열린 날은 바꿀 수 없어요");
		}
	}

	private AdminPick toPick(LocalDate date, AdminPickRow row, Instant now) {
		AdminPick.Place place = row == null ? null
				: new AdminPick.Place(row.placeId(), row.slug(), row.name(), photoUrls.of(row.photo()));
		return new AdminPick(date, PickTime.weekdayName(date), row == null ? null : row.line(),
				PickTime.opened(date, now), place);
	}

}
