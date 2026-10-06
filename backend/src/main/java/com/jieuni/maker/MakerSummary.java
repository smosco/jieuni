package com.jieuni.maker;

import com.jieuni.place.Photo;

public record MakerSummary(Long id, String slug, String name, MakerKind kind, String region, Photo photo) {
}
