package com.jieuni.follow;

import com.jieuni.common.CursorPage;
import com.jieuni.maker.MakerSummary;
import com.jieuni.user.LoginUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me")
class FollowController {

	private final FollowService followService;

	FollowController(FollowService followService) {
		this.followService = followService;
	}

	@PutMapping("/follows/{makerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void follow(@AuthenticationPrincipal LoginUser user, @PathVariable long makerId) {
		followService.follow(user.id(), makerId);
	}

	@DeleteMapping("/follows/{makerId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void unfollow(@AuthenticationPrincipal LoginUser user, @PathVariable long makerId) {
		followService.unfollow(user.id(), makerId);
	}

	@GetMapping("/following/places")
	CursorPage<FollowingPlace> places(@AuthenticationPrincipal LoginUser user,
			@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
		return followService.places(user.id(), cursor, size);
	}

	record MakersResponse(List<MakerSummary> items) {
	}

	@GetMapping("/following/makers")
	MakersResponse makers(@AuthenticationPrincipal LoginUser user) {
		return new MakersResponse(followService.makers(user.id()));
	}

	@PostMapping("/following/seen")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void seen(@AuthenticationPrincipal LoginUser user) {
		followService.markSeen(user.id());
	}

}
