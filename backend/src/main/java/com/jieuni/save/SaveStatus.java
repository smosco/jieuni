package com.jieuni.save;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

import java.util.Arrays;
import java.util.Optional;

public enum SaveStatus {

	WANT("want"),
	BEEN("been");

	@EnumeratedValue
	private final String value;

	SaveStatus(String value) {
		this.value = value;
	}

	public static Optional<SaveStatus> fromValue(String value) {
		return Arrays.stream(values()).filter(s -> s.value.equals(value)).findFirst();
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

}
