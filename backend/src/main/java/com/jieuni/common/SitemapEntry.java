package com.jieuni.common;

import java.time.Instant;

public record SitemapEntry(String slug, Instant updatedAt) {
}
