package com.jieuni.save;

import com.jieuni.place.PlaceKind;
import com.jieuni.place.PlacePhoto;
import com.jieuni.place.PlaceStatus;

import java.time.Instant;

// 내 저장 목록 한 행. 정렬·커서 키는 저장(갱신) 시각이다.
public record SavedPlaceRow(Long id, String slug, String name, String region, PlaceKind kind, PlaceStatus status,
		Instant savedAt, PlacePhoto photo) {
}
