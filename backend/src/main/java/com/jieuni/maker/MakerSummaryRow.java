package com.jieuni.maker;

import java.time.Instant;

// 만든 사람 목록 한 줄. latestPublishedAt은 정렬·커서 키.
public record MakerSummaryRow(Long id, String slug, String name, MakerKind kind, String region,
		Instant latestPublishedAt) {
}
