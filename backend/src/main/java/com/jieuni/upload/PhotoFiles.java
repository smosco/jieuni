package com.jieuni.upload;

import java.util.List;
import java.util.UUID;

// 사진 한 장 = 업로드 키 하나 + 크기별 파일 두 개({key}-1600.webp, {key}-800.webp). 공개 URL(PhotoUrls)도 같은 이름을 쓴다.
public final class PhotoFiles {

	public static final String PREFIX = "uploads/";
	private static final String W1600 = "-1600.webp";
	private static final String W800 = "-800.webp";

	private PhotoFiles() {
	}

	static String newKey(long placeId) {
		return PREFIX + placeId + "/" + UUID.randomUUID();
	}

	// 이 공간에 발급한 모양의 키인지. 다른 공간의 업로드나 임의 경로를 등록하지 못하게 한다.
	public static boolean belongsTo(String objectKey, long placeId) {
		return objectKey.matches("^" + PREFIX + placeId + "/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
	}

	public static String w1600(String objectKey) {
		return objectKey + W1600;
	}

	public static String w800(String objectKey) {
		return objectKey + W800;
	}

	public static List<String> files(String objectKey) {
		return List.of(w1600(objectKey), w800(objectKey));
	}

	// 파일 이름에서 업로드 키로. 규칙 밖의 이름은 null.
	static String keyOf(String file) {
		if (file.endsWith(W1600)) {
			return file.substring(0, file.length() - W1600.length());
		}
		if (file.endsWith(W800)) {
			return file.substring(0, file.length() - W800.length());
		}
		return null;
	}

}
