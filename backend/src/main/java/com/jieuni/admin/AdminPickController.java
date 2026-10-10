package com.jieuni.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

import static com.jieuni.admin.AdminPlaceController.clean;

@RestController
@RequestMapping("/api/admin/picks")
class AdminPickController {

	private final AdminPickService adminPickService;

	AdminPickController(AdminPickService adminPickService) {
		this.adminPickService = adminPickService;
	}

	record Items(List<AdminPick> items) {
	}

	record PickRequest(
			@NotNull(message = "공간을 골라 주세요") Long placeId,
			@NotBlank(message = "한 줄을 입력해 주세요") @Size(max = 40, message = "40자까지 쓸 수 있어요") String line) {

		PickRequest {
			line = clean(line);
		}
	}

	@GetMapping
	Items list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		return new Items(adminPickService.list(from, to));
	}

	@PutMapping("/{date}")
	AdminPick put(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@Valid @RequestBody PickRequest body) {
		return adminPickService.put(date, body.placeId(), body.line());
	}

	@DeleteMapping("/{date}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
		adminPickService.delete(date);
	}

}
