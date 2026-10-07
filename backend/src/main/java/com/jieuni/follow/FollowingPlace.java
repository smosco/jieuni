package com.jieuni.follow;

import com.jieuni.place.Photo;
import com.jieuni.place.PlaceKind;
import com.jieuni.place.PlaceStatus;

// 팔로잉 탭 격자 한 칸: PlaceTile + isNew(탭을 마지막으로 본 뒤이면서, 팔로우한 만든 사람 중 누군가를 팔로우한 뒤에 공개됐는가).
public record FollowingPlace(Long id, String slug, String name, String region, PlaceKind kind, PlaceStatus status,
		Photo photo, String caption, boolean isNew) {
}
