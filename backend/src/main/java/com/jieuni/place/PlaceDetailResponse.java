package com.jieuni.place;

import com.jieuni.maker.MakerKind;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PlaceDetailResponse(Long id, String slug, String name, PlaceKind kind, String region, String address,
		BigDecimal lat, BigDecimal lng, Access access, String visitNote, String summary, String impression,
		Senses senses, String essay, String visitedByEditorOn, PlaceStatus status, Instant publishedAt,
		List<Credit> credits, List<Source> sources, List<Photo> photos, List<Embed> embeds, MapLinks mapLinks,
		List<PlaceTile> relatedPlaces) {

	public record Senses(String light, String sound, String scent, String touch) {
	}

	public record Credit(int position, CreditRole role, boolean verified, String sourceUrl, Maker maker) {
	}

	public record Maker(Long id, String slug, String name, MakerKind kind) {
	}

	public record Source(String title, String url, SourceType type) {
	}

	public record Embed(EmbedType type, String url) {
	}

	public record MapLinks(String naver, String kakao) {
	}

}
