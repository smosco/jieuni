package com.jieuni.upload;

import jakarta.validation.Valid;
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

	record UploadRequest(@NotNull(message = "공간을 골라 주세요") Long placeId, String contentType) {
	}

	@PostMapping("/api/admin/uploads")
	UploadService.Upload issue(@Valid @RequestBody UploadRequest body) {
		return uploadService.issue(body.placeId(), body.contentType());
	}

}
