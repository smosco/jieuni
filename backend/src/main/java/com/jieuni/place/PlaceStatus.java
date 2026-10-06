package com.jieuni.place;

import jakarta.persistence.EnumeratedValue;

public enum PlaceStatus {

	SUGGESTED("suggested"),
	DRAFT("draft"),
	PUBLISHED("published"),
	PAUSED("paused"),
	CLOSED("closed"),
	REJECTED("rejected");

	@EnumeratedValue
	private final String value;

	PlaceStatus(String value) {
		this.value = value;
	}

}
