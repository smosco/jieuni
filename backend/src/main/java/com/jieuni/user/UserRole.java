package com.jieuni.user;

import jakarta.persistence.EnumeratedValue;

public enum UserRole {

	USER("user"),
	ADMIN("admin");

	@EnumeratedValue
	private final String value;

	UserRole(String value) {
		this.value = value;
	}

}
