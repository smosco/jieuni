package com.jieuni.place;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

import java.util.Arrays;
import java.util.Optional;

public enum PlaceKind {

	CAFE("cafe"),
	BAR("bar"),
	RESTAURANT("restaurant"),
	STAY("stay"),
	GARDEN("garden"),
	MUSEUM("museum"),
	GALLERY("gallery"),
	BOOKSTORE("bookstore"),
	SHOP("shop"),
	RELIGIOUS("religious"),
	PUBLIC("public"),
	OTHER("other");

	@EnumeratedValue
	private final String value;

	PlaceKind(String value) {
		this.value = value;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

	public static Optional<PlaceKind> fromValue(String value) {
		return Arrays.stream(values()).filter(kind -> kind.value.equals(value)).findFirst();
	}

}
