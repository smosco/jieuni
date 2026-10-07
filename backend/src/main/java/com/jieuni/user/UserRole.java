package com.jieuni.user;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

public enum UserRole {

	USER("user"),
	ADMIN("admin");

	@EnumeratedValue
	private final String value;

	UserRole(String value) {
		this.value = value;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

}
