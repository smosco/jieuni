package com.jieuni.suggestion;

import com.jieuni.common.ApiException;
import com.jieuni.place.CreditRole;
import com.jieuni.place.Place;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceStatus;
import com.jieuni.user.User;
import com.jieuni.user.UserRepository;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class SuggestionService {

	static final int DAILY_LIMIT = 10;
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	private final PlaceRepository places;
	private final CreditSuggestionRepository creditSuggestions;
	private final UserRepository users;
	private final Clock clock;

	SuggestionService(PlaceRepository places, CreditSuggestionRepository creditSuggestions, UserRepository users,
			Clock clock) {
		this.places = places;
		this.creditSuggestions = creditSuggestions;
		this.users = users;
		this.clock = clock;
	}

	@Transactional
	public Created suggestPlace(long userId, String name, String region, String link, String reason,
			String makerHint) {
		User user = lockAndCheckDailyLimit(userId);
		places.findSlugsByName(name, PlaceStatus.HAS_PAGE, Limit.of(1)).stream().findFirst().ifPresent(slug -> {
			throw new DuplicatePlaceException(slug);
		});
		Place place = places.save(Place.suggested(name.strip(), region.strip(), user, reason.strip(),
				blankToNull(link), blankToNull(makerHint)));
		return new Created(place.getId(), PlaceStatus.SUGGESTED.value());
	}

	@Transactional
	public Created suggestCredit(long userId, long placeId, CreditRole role, String makerName,
			String sourceUrl, boolean isSelf) {
		Place place = places.findById(placeId)
				.filter(p -> PlaceStatus.HAS_PAGE.contains(p.getStatus()))
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공간을 찾을 수 없어요"));
		User user = lockAndCheckDailyLimit(userId);
		CreditSuggestion saved = creditSuggestions.save(
				new CreditSuggestion(user, place, role, makerName.strip(), blankToNull(sourceUrl), isSelf));
		return new Created(saved.getId(), saved.getStatus().value());
	}

	@Transactional(readOnly = true)
	public MySuggestions mine(long userId) {
		return new MySuggestions(creditSuggestions.findMyPlaces(userId), creditSuggestions.findMine(userId));
	}

	// 두 제보 합산 KST 하루 한도. 같은 사용자의 동시 요청이 함께 세고 함께 통과하지 않게 사용자 행을 잠근 뒤 센다.
	private User lockAndCheckDailyLimit(long userId) {
		User user = users.findForUpdate(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"));
		Instant startOfDay = LocalDate.now(clock.withZone(KST)).atStartOfDay(KST).toInstant();
		long today = places.countSuggestedSince(userId, startOfDay) + creditSuggestions.countSince(userId, startOfDay);
		if (today >= DAILY_LIMIT) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "오늘은 더 보낼 수 없어요. 내일 다시 알려 주세요");
		}
		return user;
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	public record Created(long id, String status) {
	}

	// 같은 이름의 공개 공간이 있으면 409와 함께 그 공간 slug를 준다(프론트가 링크를 띄운다).
	public static class DuplicatePlaceException extends RuntimeException {

		private final String slug;

		DuplicatePlaceException(String slug) {
			super("이미 소개된 공간이에요");
			this.slug = slug;
		}

		public String getSlug() {
			return slug;
		}

	}

}
