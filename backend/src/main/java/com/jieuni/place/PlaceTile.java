package com.jieuni.place;

// 격자·목록 한 칸. caption은 화면마다 다르다(프로필은 역할, 팔로잉은 만든 사람 이름).
public record PlaceTile(Long id, String slug, String name, String region, PlaceKind kind, PlaceStatus status,
		Photo photo, String caption) {

	public static PlaceTile of(PlaceTileRow row, Photo photo, String caption) {
		return new PlaceTile(row.id(), row.slug(), row.name(), row.region(), row.kind(), row.status(), photo,
				caption);
	}

}
