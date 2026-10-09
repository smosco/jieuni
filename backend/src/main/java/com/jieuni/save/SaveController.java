package com.jieuni.save;

import com.jieuni.common.ApiException;
import com.jieuni.common.CursorPage;
import com.jieuni.place.PlaceTile;
import com.jieuni.user.LoginUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/saves")
class SaveController {

	private final SaveService saveService;

	SaveController(SaveService saveService) {
		this.saveService = saveService;
	}

	record SaveRequest(@NotNull(message = "상태를 골라 주세요") SaveStatus status) {
	}

	@PutMapping("/{placeId}")
	SaveService.SaveResponse save(@AuthenticationPrincipal LoginUser user, @PathVariable long placeId,
			@Valid @RequestBody SaveRequest body) {
		return saveService.save(user.id(), placeId, body.status());
	}

	@DeleteMapping("/{placeId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unsave(@AuthenticationPrincipal LoginUser user, @PathVariable long placeId) {
		saveService.unsave(user.id(), placeId);
	}

	@GetMapping
	CursorPage<PlaceTile> list(@AuthenticationPrincipal LoginUser user, @RequestParam String status,
			@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
		SaveStatus parsed = SaveStatus.fromValue(status)
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "status는 want 또는 been이어야 해요"));
		return saveService.list(user.id(), parsed, cursor, size);
	}

}
