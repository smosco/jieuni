package com.jieuni.maker;

import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceKind;
import com.jieuni.place.PlacePhoto;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTileRow;

import java.time.Instant;

// 프로필 격자 한 칸 + 이 사람의 역할.
public record MakerPlaceRow(Long id, String slug, String name, String region, PlaceKind kind, PlaceStatus status,
		Instant publishedAt, PlacePhoto photo, CreditRole role) {

	PlaceTileRow place() {
		return new PlaceTileRow(id, slug, name, region, kind, status, publishedAt, photo);
	}

}
