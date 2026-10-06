package com.jieuni.place;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

public enum Access {

	WALK_IN("walk_in"),
	RESERVATION("reservation"),
	TICKET("ticket"),
	GUESTS_ONLY("guests_only");

	@EnumeratedValue
	private final String value;

	Access(String value) {
		this.value = value;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

}
