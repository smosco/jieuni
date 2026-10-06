package com.jieuni.maker;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

public enum MakerKind {

	PERSON("person"),
	STUDIO("studio"),
	OWNER("owner");

	@EnumeratedValue
	private final String value;

	MakerKind(String value) {
		this.value = value;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

}
