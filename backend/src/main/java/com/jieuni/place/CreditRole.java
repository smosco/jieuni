package com.jieuni.place;

import jakarta.persistence.EnumeratedValue;

public enum CreditRole {

	PLANNING("planning"),
	BRANDING("branding"),
	ARCHITECTURE("architecture"),
	INTERIOR("interior"),
	LANDSCAPE("landscape"),
	LIGHTING("lighting"),
	FURNITURE("furniture"),
	SCENT("scent"),
	PHOTOGRAPHY("photography"),
	OWNER("owner"),
	OTHER("other");

	@EnumeratedValue
	private final String value;

	CreditRole(String value) {
		this.value = value;
	}

}
