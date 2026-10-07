package com.jieuni.place;

import com.jieuni.common.ApiException;
import com.jieuni.common.Cursor;
import com.jieuni.common.CursorPage;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class PlaceService {

	private static final int RELATED_LIMIT = 6;

	private final PlaceRepository places;
	private final PhotoUrls photoUrls;

	PlaceService(PlaceRepository places, PhotoUrls photoUrls) {
		this.places = places;
		this.photoUrls = photoUrls;
	}

	public PlaceDetailResponse detail(String slug) {
		Place place = places.findWithCreditsBySlug(slug, PlaceStatus.HAS_PAGE)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공간을 찾을 수 없어요"));

		List<PlaceDetailResponse.Credit> credits = place.getCredits().stream()
				.map(c -> new PlaceDetailResponse.Credit(c.getPosition(), c.getRole(), c.isVerified(), c.getSourceUrl(),
						new PlaceDetailResponse.Maker(c.getMaker().getId(), c.getMaker().getSlug(),
								c.getMaker().getName(), c.getMaker().getKind())))
				.toList();
		List<Long> makerIds = place.getCredits().stream().map(c -> c.getMaker().getId()).distinct().toList();
		List<PlaceTile> related = makerIds.isEmpty() ? List.of()
				: places.findRelated(place.getId(), makerIds, PlaceStatus.LISTED, Limit.of(RELATED_LIMIT)).stream()
						.map(row -> tile(row, null)).toList();

		return new PlaceDetailResponse(place.getId(), place.getSlug(), place.getName(), place.getKind(),
				place.getRegion(), place.getAddress(), place.getLat(), place.getLng(), place.getAccess(),
				place.getVisitNote(), place.getSummary(), place.getImpression(),
				new PlaceDetailResponse.Senses(place.getSenseLight(), place.getSenseSound(), place.getSenseScent(),
						place.getSenseTouch()),
				place.getEssay(),
				place.getVisitedByEditorOn() == null ? null : YearMonth.from(place.getVisitedByEditorOn()).toString(),
				place.getStatus(), place.getPublishedAt(), credits,
				place.getSources().stream()
						.map(s -> new PlaceDetailResponse.Source(s.getTitle(), s.getUrl(), s.getType())).toList(),
				place.getPhotos().stream().map(photoUrls::of).toList(),
				place.getEmbeds().stream().map(e -> new PlaceDetailResponse.Embed(e.getType(), e.getUrl())).toList(),
				mapLinks(place), related);
	}

	public CursorPage<PlaceTile> browse(String kindParam, String cursorParam, Integer sizeParam) {
		Set<PlaceKind> kinds = parseKinds(kindParam);
		int size = Cursor.pageSize(sizeParam);
		Limit limit = Limit.of(size + 1);
		List<PlaceTileRow> rows = cursorParam == null
				? places.findTiles(PlaceStatus.LISTED, kinds, limit)
				: findAfter(kinds, Cursor.decode(cursorParam), limit);
		return CursorPage.of(rows, size, row -> new Cursor(row.publishedAt(), row.id()), row -> tile(row, null));
	}

	private List<PlaceTileRow> findAfter(Set<PlaceKind> kinds, Cursor cursor, Limit limit) {
		return places.findTilesAfter(PlaceStatus.LISTED, kinds, cursor.at(), cursor.id(), limit);
	}

	private PlaceTile tile(PlaceTileRow row, String caption) {
		return PlaceTile.of(row, photoUrls.of(row.photo()), caption);
	}

	// "garden,cafe" → 그 종류만. 비어 있으면 전부.
	private Set<PlaceKind> parseKinds(String kindParam) {
		if (kindParam == null || kindParam.isBlank()) {
			return EnumSet.allOf(PlaceKind.class);
		}
		Set<PlaceKind> kinds = EnumSet.noneOf(PlaceKind.class);
		for (String value : kindParam.split(",")) {
			kinds.add(PlaceKind.fromValue(value.trim()).orElseThrow(
					() -> new ApiException(HttpStatus.BAD_REQUEST, "알 수 없는 종류예요: " + value.trim())));
		}
		return kinds;
	}

	// 카카오는 공식 URL 형식(link/map, link/search), 네이버는 웹 형식 공식 문서가 없어 검색 주소로 둔다.
	private PlaceDetailResponse.MapLinks mapLinks(Place place) {
		// 카카오 link/map은 쉼표로 이름·위도·경도를 나누므로 이름 안의 쉼표도 인코딩한다.
		String name = encode(place.getName()).replace(",", "%2C");
		String kakao = place.getLat() != null && place.getLng() != null
				? "https://map.kakao.com/link/map/" + name + "," + place.getLat() + "," + place.getLng()
				: "https://map.kakao.com/link/search/" + name;
		String naverQuery = place.getRegion() == null ? place.getName() : place.getName() + " " + place.getRegion();
		return new PlaceDetailResponse.MapLinks("https://map.naver.com/p/search/" + encode(naverQuery), kakao);
	}

	private static String encode(String value) {
		return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
	}

}
