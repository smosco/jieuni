package com.jieuni.maker;

import com.jieuni.common.ApiException;
import com.jieuni.common.Cursor;
import com.jieuni.common.CursorPage;
import com.jieuni.place.PhotoUrls;
import com.jieuni.place.PlacePhoto;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTile;
import com.jieuni.place.PlaceTileRow;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class MakerService {

	private final MakerRepository makers;
	private final PhotoUrls photoUrls;

	MakerService(MakerRepository makers, PhotoUrls photoUrls) {
		this.makers = makers;
		this.photoUrls = photoUrls;
	}

	public MakerProfileResponse profile(String slug) {
		Maker maker = makers.findWithAffiliationBySlug(slug).orElseThrow(MakerService::notFound);

		List<PlaceTile> places = places(maker.getId());
		if (places.isEmpty()) {
			throw notFound();
		}

		Maker affiliation = maker.getAffiliation();
		return new MakerProfileResponse(maker.getId(), maker.getSlug(), maker.getName(), maker.getKind(),
				maker.getRegion(), maker.getIntro(), maker.getWebsite(), maker.getInstagram(),
				affiliation == null ? null
						: new MakerProfileResponse.Affiliation(affiliation.getId(), affiliation.getSlug(),
								affiliation.getName()),
				places);
	}

	// 프로필 격자: 공개 페이지가 있는 공간. 같은 공간은 한 칸이고, 첫 행이 position이 가장 앞선 역할이다.
	// 관리자 만든 사람 응답도 같은 목록을 쓴다.
	public List<PlaceTile> places(long makerId) {
		Map<Long, PlaceTile> places = new LinkedHashMap<>();
		for (MakerPlaceRow row : makers.findProfilePlaces(makerId, PlaceStatus.HAS_PAGE)) {
			places.putIfAbsent(row.place().id(), tile(row.place(), row.role().label()));
		}
		return List.copyOf(places.values());
	}

	public CursorPage<MakerSummary> browse(String cursorParam, Integer sizeParam) {
		int size = Cursor.pageSize(sizeParam);
		Limit limit = Limit.of(size + 1);
		List<MakerSummaryRow> rows;
		if (cursorParam == null) {
			rows = makers.findSummaries(PlaceStatus.LISTED, limit);
		} else {
			Cursor cursor = Cursor.decode(cursorParam);
			rows = makers.findSummariesAfter(PlaceStatus.LISTED, cursor.at(), cursor.id(), limit);
		}
		CursorPage<MakerSummaryRow> page = CursorPage.of(rows, size,
				row -> new Cursor(row.latestPublishedAt(), row.id()), row -> row);

		Map<Long, PlacePhoto> photos = new HashMap<>();
		if (!page.items().isEmpty()) {
			List<Long> ids = page.items().stream().map(MakerSummaryRow::id).toList();
			for (MakerPhotoRow row : makers.findLatestPhotos(ids, PlaceStatus.LISTED)) {
				photos.putIfAbsent(row.makerId(), row.photo());
			}
		}
		List<MakerSummary> items = page.items().stream()
				.map(row -> new MakerSummary(row.id(), row.slug(), row.name(), row.kind(), row.region(),
						photoUrls.of(photos.get(row.id()))))
				.toList();
		return new CursorPage<>(items, page.nextCursor());
	}

	private PlaceTile tile(PlaceTileRow row, String caption) {
		return PlaceTile.of(row, photoUrls.of(row.photo()), caption);
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, "만든 사람을 찾을 수 없어요");
	}

}
