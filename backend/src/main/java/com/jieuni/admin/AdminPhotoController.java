package com.jieuni.admin;

import com.jieuni.place.PhotoLicense;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.jieuni.admin.AdminPlaceController.HTTPS_URL;
import static com.jieuni.admin.AdminPlaceController.HTTPS_URL_MESSAGE;
import static com.jieuni.admin.AdminPlaceController.clean;

// 사진 파일은 브라우저가 서명 URL로 올리고(/api/admin/uploads), 여기서는 그 키를 공간에 등록한다.
@RestController
@RequestMapping("/api/admin/places/{id}/photos")
class AdminPhotoController {

	private final AdminPlaceService adminPlaceService;

	AdminPhotoController(AdminPlaceService adminPlaceService) {
		this.adminPlaceService = adminPlaceService;
	}

	record AddRequest(
			@NotBlank(message = "업로드 키가 필요해요") @Size(max = 200, message = "업로드 키가 너무 길어요") String objectKey,
			@NotNull(message = "사진 크기가 필요해요") @Min(value = 1, message = "사진 크기가 필요해요") Integer width,
			@NotNull(message = "사진 크기가 필요해요") @Min(value = 1, message = "사진 크기가 필요해요") Integer height,
			@Size(max = 200, message = "200자까지 쓸 수 있어요") String alt,
			@Size(max = 100, message = "100자까지 쓸 수 있어요") String credit,
			@NotNull(message = "사진 종류를 골라 주세요") PhotoLicense license,
			@Size(max = 500, message = "링크는 500자까지 쓸 수 있어요") @Pattern(regexp = HTTPS_URL, message = HTTPS_URL_MESSAGE) String sourceUrl) {

		AddRequest {
			objectKey = clean(objectKey);
			alt = clean(alt);
			credit = clean(credit);
			sourceUrl = clean(sourceUrl);
		}
	}

	// 공간 PATCH처럼 네 칸 전부를 보낸다. 빠지거나 빈 칸이면 비운다.
	record EditRequest(
			@Size(max = 200, message = "200자까지 쓸 수 있어요") String alt,
			@Size(max = 100, message = "100자까지 쓸 수 있어요") String credit,
			@NotNull(message = "사진 종류를 골라 주세요") PhotoLicense license,
			@Size(max = 500, message = "링크는 500자까지 쓸 수 있어요") @Pattern(regexp = HTTPS_URL, message = HTTPS_URL_MESSAGE) String sourceUrl) {

		EditRequest {
			alt = clean(alt);
			credit = clean(credit);
			sourceUrl = clean(sourceUrl);
		}
	}

	record OrderRequest(@NotNull(message = "사진 목록이 맞지 않아요") List<@NotNull(message = "사진 목록이 맞지 않아요") Long> photoIds) {
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AdminPlace add(@PathVariable long id, @Valid @RequestBody AddRequest body) {
		return adminPlaceService.addPhoto(id, body.objectKey(), body.width(), body.height(),
				new AdminPlaceService.PhotoInfo(body.alt(), body.credit(), body.license(), body.sourceUrl()));
	}

	@PatchMapping("/{photoId}")
	AdminPlace edit(@PathVariable long id, @PathVariable long photoId, @Valid @RequestBody EditRequest body) {
		return adminPlaceService.editPhoto(id, photoId,
				new AdminPlaceService.PhotoInfo(body.alt(), body.credit(), body.license(), body.sourceUrl()));
	}

	@PutMapping("/order")
	AdminPlace reorder(@PathVariable long id, @Valid @RequestBody OrderRequest body) {
		return adminPlaceService.reorderPhotos(id, body.photoIds());
	}

	@DeleteMapping("/{photoId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable long id, @PathVariable long photoId) {
		adminPlaceService.deletePhoto(id, photoId);
	}

}
