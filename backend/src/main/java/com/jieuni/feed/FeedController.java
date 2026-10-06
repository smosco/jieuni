package com.jieuni.feed;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class FeedController {

	private final FeedService feedService;

	FeedController(FeedService feedService) {
		this.feedService = feedService;
	}

	@GetMapping("/api/feed")
	FeedResponse feed() {
		return feedService.feed();
	}

}
