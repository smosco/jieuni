package com.jieuni.maker;

import com.jieuni.place.PlaceTile;

import java.util.List;

public record MakerProfileResponse(Long id, String slug, String name, MakerKind kind, String region, String intro,
		String website, String instagram, Affiliation affiliation, List<PlaceTile> places) {

	public record Affiliation(Long id, String slug, String name) {
	}

}
