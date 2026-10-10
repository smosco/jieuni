package com.jieuni.admin;

import com.jieuni.common.ApiException;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.SuggestedPlaceRow;
import com.jieuni.suggestion.AdminCreditSuggestionRow;
import com.jieuni.suggestion.CreditSuggestion;
import com.jieuni.suggestion.CreditSuggestionRepository;
import com.jieuni.suggestion.SuggestionStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AdminSuggestionService {

	// 공간 제보 목록에서 고를 수 있는 상태. 제보됨·반려는 제보로만 생긴다(작성 중은 관리자가 만든 공간과 섞인다).
	private static final Set<PlaceStatus> PLACE_STATUSES = Set.of(PlaceStatus.SUGGESTED, PlaceStatus.REJECTED);

	private final PlaceRepository places;
	private final CreditSuggestionRepository credits;
	private final AdminPlaceService adminPlaceService;
	private final Clock clock;

	AdminSuggestionService(PlaceRepository places, CreditSuggestionRepository credits,
			AdminPlaceService adminPlaceService, Clock clock) {
		this.places = places;
		this.credits = credits;
		this.adminPlaceService = adminPlaceService;
		this.clock = clock;
	}

	public record User(Long id, String nickname) {

		static User of(Long id, String nickname) {
			return id == null ? null : new User(id, nickname);
		}

	}

	public record PlaceItem(Long id, String name, String region, String link, String reason, String makerHint,
			User suggestedBy, Instant createdAt) {
	}

	public record CreditItem(Long id, PlaceRef place, CreditRole role, String makerName, String sourceUrl,
			boolean isSelf, User user, SuggestionStatus status, Instant createdAt) {
	}

	public record PlaceRef(Long id, String slug, String name) {
	}

	@Transactional(readOnly = true)
	public List<PlaceItem> places(String statusParam) {
		PlaceStatus status = statusParam == null || statusParam.isBlank() ? PlaceStatus.SUGGESTED
				: PlaceStatus.fromValue(statusParam.strip()).filter(PLACE_STATUSES::contains)
						.orElseThrow(() -> unknownStatus(statusParam));
		return places.findSuggested(status).stream().map(this::toItem).toList();
	}

	@Transactional(readOnly = true)
	public List<CreditItem> credits(String statusParam) {
		SuggestionStatus status = statusParam == null || statusParam.isBlank() ? SuggestionStatus.PENDING
				: Arrays.stream(SuggestionStatus.values()).filter(s -> s.value().equals(statusParam.strip()))
						.findFirst().orElseThrow(() -> unknownStatus(statusParam));
		return credits.findForReview(status).stream().map(this::toItem).toList();
	}

	// 제보 → 작성 중. 상태 전환표의 한 칸이라 같은 규칙(제보됨이 아니면 409)을 탄다.
	@Transactional
	public AdminPlace start(long placeId) {
		return adminPlaceService.changeStatus(placeId, PlaceStatus.DRAFT);
	}

	@Transactional
	public void reject(long placeId) {
		adminPlaceService.changeStatus(placeId, PlaceStatus.REJECTED);
	}

	// 크레딧 맨 뒤에 붙인다. 목록 교체와 같은 길이라 만든 사람 확인·slug 잠금·공개 검증이 그대로 적용된다.
	@Transactional
	public AdminPlace accept(long id, long makerId, boolean verified, String sourceUrl) {
		CreditSuggestion suggestion = pending(id);
		long placeId = suggestion.getPlace().getId();
		List<AdminPlaceService.CreditLine> lines = new ArrayList<>(suggestion.getPlace().getCredits().stream()
				.map(c -> new AdminPlaceService.CreditLine(c.getMaker().getId(), c.getRole(), c.isVerified(),
						c.getSourceUrl()))
				.toList());
		int added = lines.size();
		lines.add(new AdminPlaceService.CreditLine(makerId, suggestion.getRole(), verified,
				sourceUrl != null ? sourceUrl : suggestion.getSourceUrl()));
		AdminPlace place;
		try {
			place = adminPlaceService.replaceCredits(placeId, lines);
		} catch (ApiException e) {
			throw onAddedLine(e, added);
		}
		suggestion.review(SuggestionStatus.ACCEPTED, clock.instant());
		return place;
	}

	@Transactional
	public void decline(long id) {
		pending(id).review(SuggestionStatus.DECLINED, clock.instant());
	}

	private CreditSuggestion pending(long id) {
		CreditSuggestion suggestion = credits.findById(id)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "제보를 찾을 수 없어요"));
		if (suggestion.getStatus() != SuggestionStatus.PENDING) {
			throw new ApiException(HttpStatus.CONFLICT, "이미 검토한 제보예요");
		}
		return suggestion;
	}

	// 목록 교체의 칸 오류 "credits[3].makerId"를 수락 요청의 칸 "makerId"로 바꾼다. 다른 줄의 오류는 그대로 둔다.
	private static ApiException onAddedLine(ApiException e, int index) {
		if (e.getFieldErrors() == null) {
			return e;
		}
		String prefix = "credits[" + index + "].";
		Map<String, String> errors = new LinkedHashMap<>();
		e.getFieldErrors().forEach((key, message) -> errors.put(key.startsWith(prefix) ? key.substring(prefix.length()) : key, message));
		return new ApiException(e.getStatus(), e.getMessage(), errors);
	}

	private static ApiException unknownStatus(String value) {
		return new ApiException(HttpStatus.BAD_REQUEST, "알 수 없는 상태예요: " + value.strip());
	}

	private PlaceItem toItem(SuggestedPlaceRow row) {
		return new PlaceItem(row.id(), row.name(), row.region(), row.link(), row.reason(), row.makerHint(),
				User.of(row.userId(), row.userNickname()), row.createdAt());
	}

	private CreditItem toItem(AdminCreditSuggestionRow row) {
		return new CreditItem(row.id(), new PlaceRef(row.placeId(), row.placeSlug(), row.placeName()), row.role(),
				row.makerName(), row.sourceUrl(), row.isSelf(), User.of(row.userId(), row.userNickname()),
				row.status(), row.createdAt());
	}

}
