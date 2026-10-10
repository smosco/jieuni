package com.jieuni.admin;

import com.jieuni.common.CursorPage;
import com.jieuni.place.Access;
import com.jieuni.place.PlaceEdit;
import com.jieuni.place.PlaceKind;
import com.jieuni.place.PlaceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/admin/places")
class AdminPlaceController {

	// DB CHECK(ck_places_slug)와 같은 식.
	private static final String SLUG = "^[a-z0-9]+(-[a-z0-9]+)*$";

	private final AdminPlaceService adminPlaceService;

	AdminPlaceController(AdminPlaceService adminPlaceService) {
		this.adminPlaceService = adminPlaceService;
	}

	record CreateRequest(
			@NotBlank(message = "공간 이름을 입력해 주세요") @Size(max = 100, message = "공간 이름은 100자까지 쓸 수 있어요") String name,
			@Size(max = 50, message = "지역은 50자까지 쓸 수 있어요") String region) {

		CreateRequest {
			name = clean(name);
			region = clean(region);
		}
	}

	// 임시저장: 스칼라 필드 전부를 보낸다. 빠지거나 null·빈 칸이면 비운다. 공개에 필요한 값인지는 공개할 때 따진다.
	// 문자열은 생성자에서 정리하므로 @Size·@Pattern은 정리된 값을 검사한다.
	record EditRequest(
			@Size(max = 80, message = "주소(slug)는 80자까지 쓸 수 있어요") @Pattern(regexp = SLUG, message = "영문 소문자·숫자·하이픈만 쓸 수 있어요") String slug,
			@NotBlank(message = "공간 이름을 입력해 주세요") @Size(max = 100, message = "공간 이름은 100자까지 쓸 수 있어요") String name,
			PlaceKind kind,
			@Size(max = 50, message = "지역은 50자까지 쓸 수 있어요") String region,
			@Size(max = 200, message = "주소는 200자까지 쓸 수 있어요") String address,
			@DecimalMin(value = "-90", message = "위도는 -90에서 90 사이예요") @DecimalMax(value = "90", message = "위도는 -90에서 90 사이예요") BigDecimal lat,
			@DecimalMin(value = "-180", message = "경도는 -180에서 180 사이예요") @DecimalMax(value = "180", message = "경도는 -180에서 180 사이예요") BigDecimal lng,
			Access access,
			@Size(max = 100, message = "100자까지 쓸 수 있어요") String visitNote,
			@Size(max = 60, message = "60자까지 쓸 수 있어요") String summary,
			@Size(max = 400, message = "400자까지 쓸 수 있어요") String impression,
			@Valid Senses senses,
			String essay,
			YearMonth visitedByEditorOn) {

		EditRequest {
			slug = clean(slug);
			name = clean(name);
			region = clean(region);
			address = clean(address);
			visitNote = clean(visitNote);
			summary = clean(summary);
			impression = clean(impression);
			senses = senses == null ? new Senses(null, null, null, null) : senses;
			essay = clean(essay);
		}
	}

	record Senses(
			@Size(max = 60, message = "60자까지 쓸 수 있어요") String light,
			@Size(max = 60, message = "60자까지 쓸 수 있어요") String sound,
			@Size(max = 60, message = "60자까지 쓸 수 있어요") String scent,
			@Size(max = 60, message = "60자까지 쓸 수 있어요") String touch) {

		Senses {
			light = clean(light);
			sound = clean(sound);
			scent = clean(scent);
			touch = clean(touch);
		}
	}

	record StatusRequest(@NotNull(message = "바꿀 상태를 골라 주세요") PlaceStatus to) {
	}

	@GetMapping
	CursorPage<AdminPlaceRow> list(@RequestParam(required = false) String status,
			@RequestParam(required = false) String q, @RequestParam(required = false) String cursor,
			@RequestParam(required = false) Integer size) {
		return adminPlaceService.list(status, q, cursor, size);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AdminPlace create(@Valid @RequestBody CreateRequest body) {
		return adminPlaceService.create(body.name(), body.region());
	}

	@GetMapping("/{id}")
	AdminPlace get(@PathVariable long id) {
		return adminPlaceService.get(id);
	}

	@PostMapping("/{id}/status")
	AdminPlace changeStatus(@PathVariable long id, @Valid @RequestBody StatusRequest body) {
		return adminPlaceService.changeStatus(id, body.to());
	}

	@PatchMapping("/{id}")
	AdminPlace edit(@PathVariable long id, @Valid @RequestBody EditRequest body) {
		Senses senses = body.senses();
		return adminPlaceService.edit(id, new PlaceEdit(body.slug(), body.name(), body.kind(), body.region(),
				body.address(), body.lat(), body.lng(), body.access(), body.visitNote(), body.summary(),
				body.impression(), senses.light(), senses.sound(), senses.scent(), senses.touch(), body.essay(),
				body.visitedByEditorOn() == null ? null : body.visitedByEditorOn().atDay(1)));
	}

	// 폼의 빈 칸은 값 없음. 앞뒤 공백은 지운다.
	private static String clean(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

}
