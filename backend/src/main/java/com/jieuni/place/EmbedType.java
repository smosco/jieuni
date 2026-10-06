package com.jieuni.place;

import jakarta.persistence.EnumeratedValue;

public enum EmbedType {

	YOUTUBE("youtube"),
	INSTAGRAM("instagram");

	@EnumeratedValue
	private final String value;

	EmbedType(String value) {
		this.value = value;
	}

}
