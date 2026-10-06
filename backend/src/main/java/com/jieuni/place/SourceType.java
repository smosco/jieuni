package com.jieuni.place;

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

}
