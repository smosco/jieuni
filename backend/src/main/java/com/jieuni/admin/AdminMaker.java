package com.jieuni.admin;

import com.jieuni.maker.MakerKind;
import com.jieuni.maker.MakerProfileResponse;
import com.jieuni.place.PlaceTile;

import java.time.Instant;
import java.util.List;

// GET /api/admin/makers/{id}: 공개 프로필 응답 + 관리자 값.
public record AdminMaker(Long id, String slug, String name, MakerKind kind, String region, String intro,
		String website, String instagram, MakerProfileResponse.Affiliation affiliation, List<PlaceTile> places,
		boolean slugLocked, Instant createdAt, Instant updatedAt) {
}
