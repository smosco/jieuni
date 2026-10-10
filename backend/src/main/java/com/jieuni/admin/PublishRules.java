package com.jieuni.admin;

import com.jieuni.place.Place;
import com.jieuni.place.PlacePhoto;
import com.jieuni.place.PhotoLicense;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 공개할 때만 따지는 필수 조건. DB NOT NULL 대신 여기 한 곳에 두고, 관리자 폼의 공개 검증 스키마가 같은 규칙을 1:1로 옮긴다.
// 순서는 폼 묶음 순서다. 빈 맵이면 공개할 수 있다.
@Component
class PublishRules {

	Map<String, String> check(Place place) {
		Map<String, String> errors = new LinkedHashMap<>();
		require(errors, "slug", place.getSlug(), "주소(slug)를 정해 주세요");
		require(errors, "name", place.getName(), "공간 이름을 입력해 주세요");
		require(errors, "kind", place.getKind(), "종류를 골라 주세요");
		require(errors, "region", place.getRegion(), "지역을 입력해 주세요");
		require(errors, "address", place.getAddress(), "주소를 입력해 주세요");
		require(errors, "lat", place.getLat(), "좌표를 입력해 주세요");
		require(errors, "lng", place.getLng(), "좌표를 입력해 주세요");
		require(errors, "access", place.getAccess(), "방문 방식을 골라 주세요");
		require(errors, "summary", place.getSummary(), "한 줄 소개를 입력해 주세요");
		require(errors, "impression", place.getImpression(), "감상을 입력해 주세요");

		if (place.getCredits().stream().noneMatch(c -> hasText(c.getSourceUrl()))) {
			errors.put("credits", "출처가 있는 크레딧이 1개 이상 필요해요");
		}
		if (place.getVisitedByEditorOn() == null && place.getSources().isEmpty()) {
			errors.put("sources", "머물러 본 날이 없으면 출처가 1개 이상 필요해요");
		}
		List<PlacePhoto> photos = place.getPhotos();
		if (photos.isEmpty() && place.getEmbeds().isEmpty()) {
			errors.put("photos", "사진 또는 임베드가 1개 이상 필요해요");
		}
		// 인덱스는 응답 photos 배열(position 순)과 같다.
		for (int i = 0; i < photos.size(); i++) {
			PlacePhoto photo = photos.get(i);
			require(errors, "photos[" + i + "].alt", photo.getAlt(), "사진 설명을 입력해 주세요");
			require(errors, "photos[" + i + "].credit", photo.getCredit(), "사진 출처(이름)를 입력해 주세요");
			if (photo.getLicense() != PhotoLicense.OWN && !hasText(photo.getSourceUrl())) {
				errors.put("photos[" + i + "].sourceUrl", "출처 링크를 입력해 주세요");
			}
		}
		return errors;
	}

	private static void require(Map<String, String> errors, String key, Object value, String message) {
		if (value == null || value instanceof String text && text.isBlank()) {
			errors.put(key, message);
		}
	}

	private static boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

}
