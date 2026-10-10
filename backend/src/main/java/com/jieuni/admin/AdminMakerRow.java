package com.jieuni.admin;

import com.jieuni.maker.MakerKind;

import java.time.Instant;

// GET /api/admin/makers 한 줄. placeCount는 크레딧에 있는 공간 수(상태 무관, 관리자만 본다).
public record AdminMakerRow(Long id, String slug, String name, MakerKind kind, String region, long placeCount,
		Instant updatedAt) {
}
