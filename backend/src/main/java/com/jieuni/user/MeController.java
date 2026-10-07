package com.jieuni.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
class MeController {

	private final UserService userService;

	MeController(UserService userService) {
		this.userService = userService;
	}

	record NicknameRequest(
			@NotBlank(message = "닉네임을 입력해 주세요")
			@Size(max = UserService.NICKNAME_MAX, message = "닉네임은 20자까지 쓸 수 있어요") String nickname) {
	}

	@GetMapping
	MeResponse me(@AuthenticationPrincipal LoginUser user) {
		return userService.me(user.id());
	}

	@PatchMapping
	MeResponse changeNickname(@AuthenticationPrincipal LoginUser user, @Valid @RequestBody NicknameRequest body) {
		return userService.changeNickname(user.id(), body.nickname());
	}

}
