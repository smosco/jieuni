package com.jieuni.place;

import com.jieuni.common.CursorPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class PlaceController {

	private final PlaceService placeService;

	PlaceController(PlaceService placeService) {
		this.placeService = placeService;
	}

	@GetMapping("/api/places/{slug}")
	PlaceDetailResponse detail(@PathVariable String slug) {
		return placeService.detail(slug);
	}

	@GetMapping("/api/places")
	CursorPage<PlaceTile> browse(@RequestParam(required = false) String kind,
			@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
		return placeService.browse(kind, cursor, size);
	}

}
