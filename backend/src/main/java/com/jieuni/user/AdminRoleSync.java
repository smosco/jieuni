package com.jieuni.user;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// ADMIN_EMAILS는 재시작으로만 바뀌므로 기동 때 한 번 맞추면 된다.
@Component
class AdminRoleSync implements ApplicationRunner {

	private final UserService userService;

	AdminRoleSync(UserService userService) {
		this.userService = userService;
	}

	@Override
	public void run(ApplicationArguments args) {
		userService.syncAdminRoles();
	}

}
