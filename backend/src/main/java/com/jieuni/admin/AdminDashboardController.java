package com.jieuni.admin;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class AdminDashboardController {

	private final AdminDashboardService adminDashboardService;

	AdminDashboardController(AdminDashboardService adminDashboardService) {
		this.adminDashboardService = adminDashboardService;
	}

	@GetMapping("/api/admin/dashboard")
	AdminDashboardService.Dashboard get() {
		return adminDashboardService.get();
	}

}
