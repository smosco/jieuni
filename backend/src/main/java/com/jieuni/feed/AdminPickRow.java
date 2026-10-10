package com.jieuni.feed;

import com.jieuni.place.PlacePhoto;

import java.time.LocalDate;

public record AdminPickRow(LocalDate date, String line, Long placeId, String slug, String name, PlacePhoto photo) {
}
