package com.jieuni.admin;

import com.jieuni.place.Access;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PhotoLicense;
import com.jieuni.place.Photo;
import com.jieuni.place.PlaceDetailResponse;
import com.jieuni.place.PlaceKind;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// 관리자 폼·미리보기가 쓰는 공간 한 곳. 공개 상세의 필드 전부에 관리용 값을 더했다.
// 미리보기는 이 응답을 공개 상세 컴포넌트에 그대로 넣으므로 공개 상세와 이름·모양을 맞춘다.
public record AdminPlace(Long id, String slug, String name, PlaceKind kind, String region, String address,
		BigDecimal lat, BigDecimal lng, Access access, String visitNote, String summary, String impression,
		PlaceDetailResponse.Senses senses, String essay, String visitedByEditorOn, PlaceStatus status,
		Instant publishedAt, List<Credit> credits, List<PlaceDetailResponse.Source> sources, List<AdminPhoto> photos,
		List<PlaceDetailResponse.Embed> embeds, PlaceDetailResponse.MapLinks mapLinks, List<PlaceTile> relatedPlaces,
		SuggestedBy suggestedBy, String suggestionReason, String suggestionLink, String suggestionMakerHint,
		Instant createdAt, Instant updatedAt, boolean slugLocked) {

	public record Credit(int position, CreditRole role, boolean verified, String sourceUrl,
			PlaceDetailResponse.Maker maker, Long makerId) {
	}

	public record AdminPhoto(Long id, String objectKey, int position, Photo.Urls url, int width, int height,
			String alt, String credit, PhotoLicense license, String sourceUrl) {
	}

	public record SuggestedBy(Long id, String nickname) {
	}

}
