package com.jieuni.place;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

import java.util.Arrays;
import java.util.Map;
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

	// 허용하는 상태 전환. 공개한 공간은 작성 중으로 되돌리지 않는다(저장한 사용자가 있다). 내리려면 폐업.
	// 폐업·반려는 끝 상태다.
	private static final Map<PlaceStatus, Set<PlaceStatus>> NEXT = Map.of(
			SUGGESTED, Set.of(DRAFT, REJECTED),
			DRAFT, Set.of(PUBLISHED),
			PUBLISHED, Set.of(PAUSED, CLOSED),
			PAUSED, Set.of(PUBLISHED, CLOSED));

	public boolean canBecome(PlaceStatus to) {
		return NEXT.getOrDefault(this, Set.of()).contains(to);
	}

	public static Optional<PlaceStatus> fromValue(String value) {
		return Arrays.stream(values()).filter(status -> status.value.equals(value)).findFirst();
	}

}
