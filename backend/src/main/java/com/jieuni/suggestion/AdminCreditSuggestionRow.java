package com.jieuni.suggestion;

import com.jieuni.place.CreditRole;

import java.time.Instant;

public record AdminCreditSuggestionRow(Long id, Long placeId, String placeSlug, String placeName, CreditRole role,
		String makerName, String sourceUrl, boolean isSelf, Long userId, String userNickname, SuggestionStatus status,
		Instant createdAt) {
}
