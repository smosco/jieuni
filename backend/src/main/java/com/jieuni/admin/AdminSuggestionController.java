package com.jieuni.admin;

import com.jieuni.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jieuni.admin.AdminPlaceController.HTTP_URL;
import static com.jieuni.admin.AdminPlaceController.clean;

@RestController
@RequestMapping("/api/admin/suggestions")
class AdminSuggestionController {

	private final AdminSuggestionService adminSuggestionService;

	AdminSuggestionController(AdminSuggestionService adminSuggestionService) {
		this.adminSuggestionService = adminSuggestionService;
	}

	record Items(List<?> items) {
	}

	// 출처가 없으면 제보에 적힌 출처를 쓴다.
	record AcceptRequest(
			@NotNull(message = "만든 사람을 골라 주세요") Long makerId,
			boolean verified,
			@Size(max = 500, message = "링크는 500자까지 쓸 수 있어요") @Pattern(regexp = HTTP_URL, message = "http로 시작하는 링크를 넣어 주세요") String sourceUrl) {

		AcceptRequest {
			sourceUrl = clean(sourceUrl);
		}
	}

	@GetMapping
	Items list(@RequestParam(required = false) String type, @RequestParam(required = false) String status) {
		return switch (type == null ? "" : type) {
			case "places" -> new Items(adminSuggestionService.places(status));
			case "credits" -> new Items(adminSuggestionService.credits(status));
			default -> throw new ApiException(HttpStatus.BAD_REQUEST, "type은 places나 credits예요");
		};
	}

	@PostMapping("/places/{placeId}/start")
	AdminPlace start(@PathVariable long placeId) {
		return adminSuggestionService.start(placeId);
	}

	@PostMapping("/places/{placeId}/reject")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void reject(@PathVariable long placeId) {
		adminSuggestionService.reject(placeId);
	}

	@PostMapping("/credits/{id}/accept")
	AdminPlace accept(@PathVariable long id, @Valid @RequestBody AcceptRequest body) {
		return adminSuggestionService.accept(id, body.makerId(), body.verified(), body.sourceUrl());
	}

	@PostMapping("/credits/{id}/decline")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void decline(@PathVariable long id) {
		adminSuggestionService.decline(id);
	}

}
