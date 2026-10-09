package com.jieuni.follow;

import com.jieuni.maker.MakerKind;

public record FollowedMakerRow(Long id, String slug, String name, MakerKind kind, String region) {
}
