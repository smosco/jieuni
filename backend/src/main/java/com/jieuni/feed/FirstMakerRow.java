package com.jieuni.feed;

import com.jieuni.place.CreditRole;

public record FirstMakerRow(Long placeId, Long id, String slug, String name, CreditRole role) {
}
