package com.jieuni.suggestion;

import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;

import java.time.Instant;
import java.util.List;

// 내가 보낸 제보. 상태는 그대로 주고 화면 문구(검토 중·소개됨·보류)는 프론트가 정한다.
public record MySuggestions(List<Place> places, List<Credit> credits) {

	public record Place(Long id, String name, String region, PlaceStatus status, Instant createdAt) {
	}

	public record Credit(Long id, String placeName, CreditRole role, String makerName, SuggestionStatus status) {
	}

}
