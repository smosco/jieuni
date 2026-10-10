package com.jieuni.admin;

import com.jieuni.place.Photo;

import java.time.LocalDate;

// 편성 한 칸. 빈 칸은 place·line이 null이다. locked = 이미 열린 칸(지난 날, 오늘 08:00 뒤)이라 고칠 수 없다.
public record AdminPick(LocalDate date, String weekday, String line, boolean locked, Place place) {

	public record Place(Long id, String slug, String name, Photo photo) {
	}

}
