package com.jieuni.suggestion;

import com.jieuni.place.CreditRole;
import com.jieuni.user.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class SuggestionController {

	// 관리자가 눌러 보는 링크라 http·https만 받는다(javascript: 같은 주소를 막는다). 빈 값은 링크 없음.
	private static final String HTTP_URL = "^$|^https?://\\S+$";

	private final SuggestionService suggestionService;

	SuggestionController(SuggestionService suggestionService) {
		this.suggestionService = suggestionService;
	}

	record PlaceRequest(
			@NotBlank(message = "공간 이름을 입력해 주세요") @Size(max = 100, message = "공간 이름은 100자까지 쓸 수 있어요") String name,
			@NotBlank(message = "지역을 입력해 주세요") @Size(max = 50, message = "지역은 50자까지 쓸 수 있어요") String region,
			@Size(max = 500, message = "링크는 500자까지 쓸 수 있어요") @Pattern(regexp = HTTP_URL, message = "http로 시작하는 링크를 넣어 주세요") String link,
			@NotBlank(message = "오래 생각나는 이유를 적어 주세요") @Size(max = 300, message = "300자까지 쓸 수 있어요") String reason,
			@Size(max = 100, message = "100자까지 쓸 수 있어요") String makerHint) {
	}

	record CreditRequest(
			@NotNull(message = "공간을 골라 주세요") Long placeId,
			@NotNull(message = "역할을 골라 주세요") CreditRole role,
			@NotBlank(message = "이름을 입력해 주세요") @Size(max = 100, message = "이름은 100자까지 쓸 수 있어요") String makerName,
			@Size(max = 500, message = "링크는 500자까지 쓸 수 있어요") @Pattern(regexp = HTTP_URL, message = "http로 시작하는 링크를 넣어 주세요") String sourceUrl,
			Boolean isSelf) {
	}

	record DuplicatePlace(String message, String slug) {
	}

	@PostMapping("/api/suggestions/places")
	@ResponseStatus(HttpStatus.CREATED)
	SuggestionService.Created suggestPlace(@AuthenticationPrincipal LoginUser user,
			@Valid @RequestBody PlaceRequest body) {
		return suggestionService.suggestPlace(user.id(), body.name(), body.region(), body.link(), body.reason(),
				body.makerHint());
	}

	@PostMapping("/api/suggestions/credits")
	@ResponseStatus(HttpStatus.CREATED)
	SuggestionService.Created suggestCredit(@AuthenticationPrincipal LoginUser user,
			@Valid @RequestBody CreditRequest body) {
		return suggestionService.suggestCredit(user.id(), body.placeId(), body.role(), body.makerName(),
				body.sourceUrl(), Boolean.TRUE.equals(body.isSelf())); // 빠지면 "본인 아님"
	}

	@GetMapping("/api/me/suggestions")
	MySuggestions mine(@AuthenticationPrincipal LoginUser user) {
		return suggestionService.mine(user.id());
	}

	@ExceptionHandler(SuggestionService.DuplicatePlaceException.class)
	ResponseEntity<DuplicatePlace> duplicate(SuggestionService.DuplicatePlaceException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new DuplicatePlace(e.getMessage(), e.getSlug()));
	}

}
