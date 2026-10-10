package com.jieuni.place;

import java.time.Instant;

public record SuggestedPlaceRow(Long id, String name, String region, String link, String reason, String makerHint,
		Long userId, String userNickname, PlaceStatus status, Instant createdAt) {
}
