package com.jieuni.place;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

public enum SourceType {

	ARTICLE("article"),
	VIDEO("video"),
	INSTAGRAM("instagram"),
	OFFICIAL("official");

	@EnumeratedValue
	private final String value;

	SourceType(String value) {
		this.value = value;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

}
