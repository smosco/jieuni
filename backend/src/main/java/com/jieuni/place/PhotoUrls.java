package com.jieuni.place;

import com.jieuni.upload.PhotoFiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 사진 응답은 완성된 URL을 준다: {PHOTO_PUBLIC_BASE_URL}/{objectKey}-1600.webp. 프론트가 저장소 주소를 몰라도 되고, 이미 변환된 WebP라 Next 이미지 최적화를 거치지 않는다.
@Component
public class PhotoUrls {

	private final String baseUrl;

	PhotoUrls(@Value("${jieuni.photo-public-base-url}") String baseUrl) {
		this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
	}

	public Photo of(PlacePhoto photo) {
		if (photo == null) {
			return null;
		}
		String prefix = baseUrl + "/";
		return new Photo(new Photo.Urls(prefix + PhotoFiles.w1600(photo.getObjectKey()),
				prefix + PhotoFiles.w800(photo.getObjectKey())), photo.getWidth(),
				photo.getHeight(), photo.getAlt(), photo.getCredit(), photo.getLicense(), photo.getSourceUrl());
	}

}
