package com.jieuni.common;

import java.util.List;
import java.util.function.Function;

public record CursorPage<T>(List<T> items, String nextCursor) {

	// size + 1개를 읽어 하나가 더 있으면 다음 커서를 만든다.
	public static <R, T> CursorPage<T> of(List<R> rows, int size, Function<R, Cursor> cursorOf, Function<R, T> toItem) {
		boolean hasNext = rows.size() > size;
		List<R> page = hasNext ? rows.subList(0, size) : rows;
		String next = hasNext ? cursorOf.apply(page.getLast()).encode() : null;
		return new CursorPage<>(page.stream().map(toItem).toList(), next);
	}

}
