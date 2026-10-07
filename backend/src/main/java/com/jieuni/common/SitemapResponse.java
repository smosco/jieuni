package com.jieuni.common;

import java.util.List;

public record SitemapResponse(List<SitemapEntry> places, List<SitemapEntry> makers) {
}
