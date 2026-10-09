package com.jieuni.admin;

import com.jieuni.place.PlaceKind;
import com.jieuni.place.PlaceStatus;

import java.time.Instant;

// 관리자 공간 목록 한 줄. 대표 사진은 있는지만 본다.
public record AdminPlaceRow(Long id, String slug, String name, String region, PlaceKind kind, PlaceStatus status,
		Instant updatedAt, boolean hasPhoto) {
}
