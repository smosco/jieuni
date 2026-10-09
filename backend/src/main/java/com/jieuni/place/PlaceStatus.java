package com.jieuni.place;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public enum PlaceStatus {

	SUGGESTED("suggested"),
	DRAFT("draft"),
	PUBLISHED("published"),
	PAUSED("paused"),
	CLOSED("closed"),
	REJECTED("rejected");

	// 공개 페이지(상세·프로필 격자·sitemap)가 있는 상태.
	public static final Set<PlaceStatus> HAS_PAGE = Set.of(PUBLISHED, PAUSED, CLOSED);

	// 목록(둘러보기·관련 공간·만든 사람 목록)에 나오는 상태. 폐업은 빠진다.
	public static final Set<PlaceStatus> LISTED = Set.of(PUBLISHED, PAUSED);

	@EnumeratedValue
	private final String value;

	PlaceStatus(String value) {
		this.value = value;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

	public static Optional<PlaceStatus> fromValue(String value) {
		return Arrays.stream(values()).filter(status -> status.value.equals(value)).findFirst();
	}

}
