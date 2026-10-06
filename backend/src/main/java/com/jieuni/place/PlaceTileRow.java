package com.jieuni.place;

import java.time.Instant;

// 목록 쿼리 한 행. 대표 사진(position 0)은 같은 쿼리의 left join으로 함께 온다.
public record PlaceTileRow(Long id, String slug, String name, String region, PlaceKind kind, PlaceStatus status,
		Instant publishedAt, PlacePhoto photo) {
}
