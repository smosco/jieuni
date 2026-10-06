package com.jieuni.save;

import jakarta.persistence.EnumeratedValue;

public enum SaveStatus {

	WANT("want"),
	BEEN("been");

	@EnumeratedValue
	private final String value;

	SaveStatus(String value) {
		this.value = value;
	}

}
