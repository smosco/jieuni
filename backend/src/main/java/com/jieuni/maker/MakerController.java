package com.jieuni.maker;

import com.jieuni.common.CursorPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MakerController {

	private final MakerService makerService;

	MakerController(MakerService makerService) {
		this.makerService = makerService;
	}

	@GetMapping("/api/makers/{slug}")
	MakerProfileResponse profile(@PathVariable String slug) {
		return makerService.profile(slug);
	}

	@GetMapping("/api/makers")
	CursorPage<MakerSummary> browse(@RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer size) {
		return makerService.browse(cursor, size);
	}

}
