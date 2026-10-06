package com.jieuni.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SitemapController {

	private final SitemapService sitemapService;

	SitemapController(SitemapService sitemapService) {
		this.sitemapService = sitemapService;
	}

	@GetMapping("/api/sitemap")
	SitemapResponse sitemap() {
		return sitemapService.sitemap();
	}

}
