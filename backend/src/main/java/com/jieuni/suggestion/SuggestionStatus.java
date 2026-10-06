package com.jieuni.suggestion;

import jakarta.persistence.EnumeratedValue;

public enum SuggestionStatus {

	PENDING("pending"),
	ACCEPTED("accepted"),
	DECLINED("declined");

	@EnumeratedValue
	private final String value;

	SuggestionStatus(String value) {
		this.value = value;
	}

}
