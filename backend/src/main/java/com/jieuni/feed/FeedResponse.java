package com.jieuni.feed;

import com.fasterxml.jackson.annotation.JsonValue;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceTile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

// API.md 1절 GET /api/feed. 시간 계산은 전부 서버가 하고 프론트는 이 값을 그린다.
public record FeedResponse(LocalDate weekLabel, List<Pick> picks, int openedCount, Instant nextOpensAt,
		String nextWeekday, NextRelative nextRelative) {

	// 마지막 화면 문구를 고르는 값(SPEC 5절). 다음 공개일이 오늘이면 today, 내일이면 tomorrow, 그 밖은 later.
	public enum NextRelative {

		TODAY("today"), TOMORROW("tomorrow"), LATER("later");

		private final String value;

		NextRelative(String value) {
			this.value = value;
		}

		@JsonValue
		public String value() {
			return value;
		}

	}

	public record Pick(LocalDate date, String weekday, String line, PlaceTile place, FirstMaker firstMaker) {
	}

	public record FirstMaker(Long id, String slug, String name, CreditRole role) {
	}

}
