package com.jieuni.admin;

import com.jieuni.common.CursorPage;
import com.jieuni.maker.MakerKind;
import jakarta.validation.Valid;
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

import static com.jieuni.admin.AdminPlaceController.HTTP_URL;
import static com.jieuni.admin.AdminPlaceController.SLUG;
import static com.jieuni.admin.AdminPlaceController.clean;

@RestController
@RequestMapping("/api/admin/makers")
class AdminMakerController {

	private final AdminMakerService adminMakerService;

	AdminMakerController(AdminMakerService adminMakerService) {
		this.adminMakerService = adminMakerService;
	}

	// 만들기와 수정이 같은 모양이다. 수정도 필드 전부를 보낸다(빠지거나 null·빈 칸이면 비움). 공간 PATCH와 같은 이유.
	record MakerRequest(
			@Size(max = 80, message = "주소(slug)는 80자까지 쓸 수 있어요") @Pattern(regexp = SLUG, message = "영문 소문자·숫자·하이픈만 쓸 수 있어요") String slug,
			@NotBlank(message = "이름을 입력해 주세요") @Size(max = 100, message = "이름은 100자까지 쓸 수 있어요") String name,
			@NotNull(message = "종류를 골라 주세요") MakerKind kind,
			@Size(max = 50, message = "지역은 50자까지 쓸 수 있어요") String region,
			@Size(max = 200, message = "소개는 200자까지 쓸 수 있어요") String intro,
			@Size(max = 500, message = "링크는 500자까지 쓸 수 있어요") @Pattern(regexp = HTTP_URL, message = "http로 시작하는 링크를 넣어 주세요") String website,
			// 공개 프로필이 https://www.instagram.com/{아이디}/로 링크한다.
			@Pattern(regexp = "^[A-Za-z0-9._]{1,30}$", message = "인스타그램 아이디만 넣어 주세요") String instagram,
			Long affiliationId) {

		MakerRequest {
			slug = clean(slug);
			name = clean(name);
			region = clean(region);
			intro = clean(intro);
			website = clean(website);
			instagram = clean(instagram);
			if (instagram != null && instagram.startsWith("@")) {
				instagram = clean(instagram.substring(1));
			}
		}

		AdminMakerService.MakerForm toForm() {
			return new AdminMakerService.MakerForm(slug, name, kind, region, intro, website, instagram, affiliationId);
		}
	}

	@GetMapping
	CursorPage<AdminMakerRow> list(@RequestParam(required = false) String q,
			@RequestParam(required = false) String cursor, @RequestParam(required = false) Integer size) {
		return adminMakerService.list(q, cursor, size);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AdminMaker create(@Valid @RequestBody MakerRequest body) {
		return adminMakerService.create(body.toForm());
	}

	@GetMapping("/{id}")
	AdminMaker get(@PathVariable long id) {
		return adminMakerService.get(id);
	}

	@PatchMapping("/{id}")
	AdminMaker edit(@PathVariable long id, @Valid @RequestBody MakerRequest body) {
		return adminMakerService.edit(id, body.toForm());
	}

}
