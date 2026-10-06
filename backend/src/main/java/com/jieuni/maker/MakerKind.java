package com.jieuni.maker;

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

}
