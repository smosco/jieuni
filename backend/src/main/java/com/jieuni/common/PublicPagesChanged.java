package com.jieuni.common;

import com.jieuni.maker.Maker;
import com.jieuni.place.Place;
import com.jieuni.place.PlaceTile;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

// 커밋되면 다시 만들 공개 페이지의 캐시 태그. 태그 이름은 프론트 features/*/api.ts와 같다.
public record PublicPagesChanged(Set<String> tags) {

	private static final String FEED = "feed";
	private static final String BROWSE = "browse";
	private static final String SITEMAP = "sitemap";

	// 공간은 상세, 크레딧에 든 사람의 프로필 격자, 둘러보기, 피드 카드에 보인다.
	public static PublicPagesChanged place(Place place, Collection<String> previousMakerSlugs, boolean firstPublish) {
		Set<String> tags = new LinkedHashSet<>();
		tags.add("place:" + place.getSlug());
		place.getCredits().stream().map(c -> c.getMaker().getSlug()).filter(Objects::nonNull)
				.forEach(slug -> tags.add("maker:" + slug));
		// 크레딧을 바꾸면 교체 전 사람도 넣는다. 빠진 사람의 프로필 격자에서도 이 공간이 사라진다.
		previousMakerSlugs.stream().filter(Objects::nonNull).forEach(slug -> tags.add("maker:" + slug));
		tags.add(BROWSE);
		tags.add(FEED);
		if (firstPublish) {
			tags.add(SITEMAP);
		}
		return new PublicPagesChanged(tags);
	}

	// 만든 사람 이름·slug는 프로필, 크레딧에 든 공간의 상세, 둘러보기, 피드 카드의 첫 만든 사람에 보인다.
	public static PublicPagesChanged maker(Maker maker, String previousSlug, Collection<PlaceTile> places,
			boolean firstPublic) {
		Set<String> tags = new LinkedHashSet<>();
		// 잠기기 전에 slug를 바꿨으면 옛 주소의 캐시도 비운다.
		Stream.of(maker.getSlug(), previousSlug).filter(Objects::nonNull).forEach(slug -> tags.add("maker:" + slug));
		places.forEach(place -> tags.add("place:" + place.slug()));
		tags.add(BROWSE);
		tags.add(FEED);
		if (firstPublic) {
			tags.add(SITEMAP);
		}
		return new PublicPagesChanged(tags);
	}

	public static PublicPagesChanged feed() {
		return new PublicPagesChanged(Set.of(FEED));
	}

}
