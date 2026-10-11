package com.jieuni.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
class MeController {

	private final UserService userService;

	private final MeStateService meStateService;

	MeController(UserService userService, MeStateService meStateService) {
		this.userService = userService;
		this.meStateService = meStateService;
	}

	record NicknameRequest(
			@NotBlank(message = "닉네임을 입력해 주세요")
			@Size(max = UserService.NICKNAME_MAX, message = "닉네임은 20자까지 쓸 수 있어요") String nickname) {
	}

	@GetMapping
	MeResponse me(@AuthenticationPrincipal LoginUser user) {
		return userService.me(user.id());
	}

	@GetMapping("/state")
	MeStateService.MeState state(@AuthenticationPrincipal LoginUser user,
			@RequestParam(required = false) String places, @RequestParam(required = false) String makers) {
		return meStateService.state(user.id(), places, makers);
	}

	@PatchMapping
	MeResponse changeNickname(@AuthenticationPrincipal LoginUser user, @Valid @RequestBody NicknameRequest body) {
		return userService.changeNickname(user.id(), body.nickname());
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void withdraw(@AuthenticationPrincipal LoginUser user, HttpServletRequest request, HttpServletResponse response) {
		userService.withdraw(user);
		// 이 요청의 세션 행은 이미 지웠다. 무효화해야 요청 끝에 세션을 다시 저장하지 않고 SESSION 쿠키도 지운다.
		new SecurityContextLogoutHandler().logout(request, response, null);
	}

}
