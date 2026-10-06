package com.jieuni.suggestion;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

public enum SuggestionStatus {

	PENDING("pending"),
	ACCEPTED("accepted"),
	DECLINED("declined");

	@EnumeratedValue
	private final String value;

	SuggestionStatus(String value) {
		this.value = value;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

}
