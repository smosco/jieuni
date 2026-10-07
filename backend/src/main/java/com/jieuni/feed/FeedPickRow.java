package com.jieuni.feed;

import com.jieuni.place.PlaceTileRow;

import java.time.LocalDate;

public record FeedPickRow(LocalDate date, String line, PlaceTileRow place) {
}
