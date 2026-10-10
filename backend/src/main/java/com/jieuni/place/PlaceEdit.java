package com.jieuni.place;

import java.math.BigDecimal;
import java.time.LocalDate;

// 관리자 폼의 임시저장 한 번. 스칼라 필드 전부를 한 번에 덮어쓴다(null은 비움).
public record PlaceEdit(String slug, String name, PlaceKind kind, String region, String address, BigDecimal lat,
		BigDecimal lng, Access access, String visitNote, String summary, String impression, String senseLight,
		String senseSound, String senseScent, String senseTouch, String essay, LocalDate visitedByEditorOn) {
}
