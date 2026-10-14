package com.jieuni.upload;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
class UploadController {

	private final UploadService uploadService;

	UploadController(UploadService uploadService) {
		this.uploadService = uploadService;
	}

	record UploadRequest(@NotNull(message = "공간을 골라 주세요") Long placeId, String contentType,
			@NotNull(message = "사진 크기가 필요해요") @Valid Sizes sizes) {
	}

	// 바꾼 WebP 두 파일의 바이트 수. 서명에 들어가 다른 크기로는 올릴 수 없다.
	record Sizes(
			@NotNull(message = "사진 크기가 필요해요") @Min(value = 1, message = "사진 크기가 필요해요") @Max(value = UploadService.MAX_BYTES, message = UploadService.TOO_LARGE) Long w1600,
			@NotNull(message = "사진 크기가 필요해요") @Min(value = 1, message = "사진 크기가 필요해요") @Max(value = UploadService.MAX_BYTES, message = UploadService.TOO_LARGE) Long w800) {
	}

	@PostMapping("/api/admin/uploads")
	UploadService.Upload issue(@Valid @RequestBody UploadRequest body) {
		return uploadService.issue(body.placeId(), body.contentType(), body.sizes().w1600(), body.sizes().w800());
	}

}
