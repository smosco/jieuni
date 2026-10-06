package com.jieuni.place;

import jakarta.persistence.EnumeratedValue;

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

}
