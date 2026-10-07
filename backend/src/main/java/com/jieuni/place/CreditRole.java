package com.jieuni.place;

import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.persistence.EnumeratedValue;

public enum CreditRole {

	PLANNING("planning", "기획"),
	BRANDING("branding", "브랜딩"),
	ARCHITECTURE("architecture", "건축"),
	INTERIOR("interior", "인테리어"),
	LANDSCAPE("landscape", "조경"),
	LIGHTING("lighting", "조명"),
	FURNITURE("furniture", "가구"),
	SCENT("scent", "향"),
	PHOTOGRAPHY("photography", "사진"),
	OWNER("owner", "주인"),
	OTHER("other", "그 밖");

	@EnumeratedValue
	private final String value;

	// 프로필 격자 캡션에 쓰는 화면 문구.
	private final String label;

	CreditRole(String value, String label) {
		this.value = value;
		this.label = label;
	}

	// DB 값과 JSON 값을 같게 둔다(소문자).
	@JsonValue
	public String value() {
		return value;
	}

	public String label() {
		return label;
	}

}
