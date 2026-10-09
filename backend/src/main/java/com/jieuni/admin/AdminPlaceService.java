package com.jieuni.admin;

import com.jieuni.common.ApiException;
import com.jieuni.common.Cursor;
import com.jieuni.common.CursorPage;
import com.jieuni.place.PhotoUrls;
import com.jieuni.place.Place;
import com.jieuni.place.PlaceDetailResponse;
import com.jieuni.place.PlaceEdit;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceService;
import com.jieuni.place.PlaceStatus;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class AdminPlaceService {

	private final PlaceRepository places;
	private final AdminPlaceRepository adminPlaces;
	private final PlaceService placeService;
	private final PhotoUrls photoUrls;

	AdminPlaceService(PlaceRepository places, AdminPlaceRepository adminPlaces, PlaceService placeService,
			PhotoUrls photoUrls) {
		this.places = places;
		this.adminPlaces = adminPlaces;
		this.placeService = placeService;
		this.photoUrls = photoUrls;
	}

	@Transactional(readOnly = true)
	public CursorPage<AdminPlaceRow> list(String statusParam, String q, String cursorParam, Integer sizeParam) {
		Set<PlaceStatus> statuses = parseStatus(statusParam);
		String pattern = q == null || q.isBlank() ? "%" : "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
		int size = Cursor.pageSize(sizeParam);
		Limit limit = Limit.of(size + 1);
		List<AdminPlaceRow> rows;
		if (cursorParam == null) {
			rows = adminPlaces.findRows(statuses, pattern, limit);
		} else {
			Cursor cursor = Cursor.decode(cursorParam);
			rows = adminPlaces.findRowsAfter(statuses, pattern, cursor.at(), cursor.id(), limit);
		}
		return CursorPage.of(rows, size, row -> new Cursor(row.updatedAt(), row.id()), row -> row);
	}

	@Transactional
	public AdminPlace create(String name, String region) {
		return toResponse(places.save(Place.draft(name, region)));
	}

	@Transactional(readOnly = true)
	public AdminPlace get(long id) {
		return toResponse(find(id));
	}

	@Transactional
	public AdminPlace edit(long id, PlaceEdit edit) {
		Place place = find(id);
		// 공개 URL은 한 번 정하면 바꾸지 않는다. 같은 값을 다시 보내는 것은 변경이 아니다.
		if (place.getPublishedAt() != null && !Objects.equals(place.getSlug(), edit.slug())) {
			throw new ApiException(HttpStatus.CONFLICT, "공개한 뒤에는 주소(slug)를 바꿀 수 없어요");
		}
		place.edit(edit);
		// slug 중복은 DB 제약이 판단한다. 커밋까지 미루지 않고 여기서 내보내 응답 전에 409로 바꾼다.
		places.flush();
		return toResponse(place);
	}

	private Place find(long id) {
		return places.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공간을 찾을 수 없어요"));
	}

	private AdminPlace toResponse(Place place) {
		List<AdminPlace.Credit> credits = place.getCredits().stream()
				.map(c -> new AdminPlace.Credit(c.getPosition(), c.getRole(), c.isVerified(), c.getSourceUrl(),
						new PlaceDetailResponse.Maker(c.getMaker().getId(), c.getMaker().getSlug(),
								c.getMaker().getName(), c.getMaker().getKind()),
						c.getMaker().getId()))
				.toList();
		List<AdminPlace.AdminPhoto> photos = place.getPhotos().stream()
				.map(p -> new AdminPlace.AdminPhoto(p.getId(), p.getObjectKey(), p.getPosition(),
						photoUrls.of(p).url(), p.getWidth(), p.getHeight(), p.getAlt(), p.getCredit(), p.getLicense(),
						p.getSourceUrl()))
				.toList();
		AdminPlace.SuggestedBy suggestedBy = place.getSuggestedBy() == null ? null
				: new AdminPlace.SuggestedBy(place.getSuggestedBy().getId(), place.getSuggestedBy().getNickname());

		return new AdminPlace(place.getId(), place.getSlug(), place.getName(), place.getKind(), place.getRegion(),
				place.getAddress(), place.getLat(), place.getLng(), place.getAccess(), place.getVisitNote(),
				place.getSummary(), place.getImpression(),
				new PlaceDetailResponse.Senses(place.getSenseLight(), place.getSenseSound(), place.getSenseScent(),
						place.getSenseTouch()),
				place.getEssay(),
				place.getVisitedByEditorOn() == null ? null : YearMonth.from(place.getVisitedByEditorOn()).toString(),
				place.getStatus(), place.getPublishedAt(), credits,
				place.getSources().stream()
						.map(s -> new PlaceDetailResponse.Source(s.getTitle(), s.getUrl(), s.getType())).toList(),
				photos,
				place.getEmbeds().stream().map(e -> new PlaceDetailResponse.Embed(e.getType(), e.getUrl())).toList(),
				placeService.mapLinks(place), placeService.related(place), suggestedBy, place.getSuggestionReason(),
				place.getSuggestionLink(), place.getSuggestionMakerHint(), place.getCreatedAt(), place.getUpdatedAt(),
				place.getPublishedAt() != null);
	}

	private static Set<PlaceStatus> parseStatus(String value) {
		if (value == null || value.isBlank()) {
			return EnumSet.allOf(PlaceStatus.class);
		}
		return EnumSet.of(PlaceStatus.fromValue(value.strip())
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "알 수 없는 상태예요: " + value.strip())));
	}

	// 검색어의 %·_를 글자 그대로 찾는다.
	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
