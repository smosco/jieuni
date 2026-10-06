package com.jieuni.common;

import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

// 커서 목록의 위치: 정렬 키 (시각, id)를 불투명 문자열로 감싼다(API.md 0절).
public record Cursor(Instant at, long id) {

	private static final int DEFAULT_SIZE = 24;
	private static final int MAX_SIZE = 60;

	public String encode() {
		String raw = at + "|" + id;
		return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
	}

	public static Cursor decode(String encoded) {
		try {
			String raw = new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
			int bar = raw.indexOf('|');
			return new Cursor(Instant.parse(raw.substring(0, bar)), Long.parseLong(raw.substring(bar + 1)));
		} catch (RuntimeException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "목록 위치(cursor)를 읽을 수 없어요");
		}
	}

	public static int pageSize(Integer requested) {
		if (requested == null) {
			return DEFAULT_SIZE;
		}
		if (requested < 1 || requested > MAX_SIZE) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "size는 1에서 " + MAX_SIZE + " 사이여야 해요");
		}
		return requested;
	}

}
