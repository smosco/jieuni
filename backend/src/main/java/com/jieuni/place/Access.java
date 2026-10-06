package com.jieuni.place;

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

}
