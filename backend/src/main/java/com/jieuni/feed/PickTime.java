package com.jieuni.feed;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

// 편성 한 칸이 열리는 때: pick_date 08:00 KST. 피드(보이는가)와 편성 관리(고칠 수 있는가)가 같은 경계를 쓴다.
public final class PickTime {

	public static final ZoneId KST = ZoneId.of("Asia/Seoul");
	public static final LocalTime OPENS_AT = LocalTime.of(8, 0);

	private PickTime() {
	}

	public static Instant opensAt(LocalDate pickDate) {
		return pickDate.atTime(OPENS_AT).atZone(KST).toInstant();
	}

	public static boolean opened(LocalDate pickDate, Instant now) {
		return !opensAt(pickDate).isAfter(now);
	}

	public static boolean weekday(LocalDate date) {
		return date.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue();
	}

	// 응답의 요일 표기: MON·TUE·…
	public static String weekdayName(LocalDate date) {
		return date.getDayOfWeek().name().substring(0, 3);
	}

}
