package com.jieuni.user;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

// 관리자 API는 세션에 든 값이 아니라 요청마다 DB의 users.role로 판단한다.
@Component
public class AdminAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

	private final UserService userService;

	AdminAuthorizationManager(UserService userService) {
		this.userService = userService;
	}

	@Override
	public AuthorizationResult authorize(Supplier<? extends Authentication> authentication,
			RequestAuthorizationContext context) {
		Authentication auth = authentication.get();
		boolean granted = auth != null && auth.getPrincipal() instanceof LoginUser user && userService.isAdmin(user.id());
		return new AuthorizationDecision(granted);
	}

}
