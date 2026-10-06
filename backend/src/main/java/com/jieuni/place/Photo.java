package com.jieuni.place;

public record Photo(Urls url, int width, int height, String alt, String credit, PhotoLicense license,
		String sourceUrl) {

	public record Urls(String w1600, String w800) {
	}

}
