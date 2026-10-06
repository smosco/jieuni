package com.jieuni.place;

import jakarta.persistence.EnumeratedValue;

public enum PhotoLicense {

	OWN("own"),
	PRESS("press"),
	CREDITED("credited");

	@EnumeratedValue
	private final String value;

	PhotoLicense(String value) {
		this.value = value;
	}

}
