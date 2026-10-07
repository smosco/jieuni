package com.jieuni.save;

import com.jieuni.common.ApiException;
import com.jieuni.common.Cursor;
import com.jieuni.common.CursorPage;
import com.jieuni.place.PhotoUrls;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTile;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class SaveService {

	private final SaveRepository saves;
	private final PlaceRepository places;
	private final PhotoUrls photoUrls;
	private final Clock clock;

	SaveService(SaveRepository saves, PlaceRepository places, PhotoUrls photoUrls, Clock clock) {
		this.saves = saves;
		this.places = places;
		this.photoUrls = photoUrls;
		this.clock = clock;
	}

	// 한 사용자·공간에 행은 하나다. want↔been은 같은 행의 상태만 바꾼다.
	@Transactional
	public SaveResponse save(long userId, long placeId, SaveStatus status) {
		PlaceStatus placeStatus = places.findStatusById(placeId)
				.filter(PlaceStatus.HAS_PAGE::contains)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공간을 찾을 수 없어요"));
		if (placeStatus == PlaceStatus.CLOSED) {
			throw new ApiException(HttpStatus.CONFLICT, "이제는 만날 수 없는 공간이라 저장할 수 없어요");
		}
		saves.upsert(userId, placeId, status.value(), clock.instant());
		return new SaveResponse(placeId, status);
	}

	@Transactional
	public void unsave(long userId, long placeId) {
		saves.deleteSave(new SaveId(userId, placeId));
	}

	// 폐업한 공간도 저장 기록은 남아 있으므로 목록에 보인다.
	@Transactional(readOnly = true)
	public CursorPage<PlaceTile> list(long userId, SaveStatus status, String cursorParam, Integer sizeParam) {
		int size = Cursor.pageSize(sizeParam);
		Limit limit = Limit.of(size + 1);
		List<SavedPlaceRow> rows;
		if (cursorParam == null) {
			rows = saves.findSavedPlaces(userId, status, PlaceStatus.HAS_PAGE, limit);
		} else {
			Cursor cursor = Cursor.decode(cursorParam);
			rows = saves.findSavedPlacesAfter(userId, status, PlaceStatus.HAS_PAGE, cursor.at(), cursor.id(), limit);
		}
		return CursorPage.of(rows, size, row -> new Cursor(row.savedAt(), row.id()),
				row -> new PlaceTile(row.id(), row.slug(), row.name(), row.region(), row.kind(), row.status(),
						photoUrls.of(row.photo()), null));
	}

	public record SaveResponse(long placeId, SaveStatus status) {
	}

}
