package com.jieuni.user;

import org.springframework.security.oauth2.core.oidc.user.OidcUser;

// 구글 사용자 정보에서 users에 넣을 값을 고른다. 제공자가 늘면 같은 모양의 record를 하나 더 둔다.
record GoogleProfile(String providerId, String email, String nickname, String profileImageUrl) {

	static final String PROVIDER = "google";

	static GoogleProfile from(OidcUser user) {
		// 검증되지 않은 이메일은 남의 주소일 수 있어 저장하지도, 관리자 목록과 비교하지도 않는다.
		String email = Boolean.TRUE.equals(user.getEmailVerified()) ? user.getEmail() : null;
		return new GoogleProfile(user.getSubject(), email, nickname(user.getFullName(), email), user.getPicture());
	}

	// 구글은 name을 보장하지 않는다.
	private static String nickname(String name, String email) {
		if (name != null && !name.isBlank()) {
			return name.strip();
		}
		if (email != null) {
			return email.substring(0, email.indexOf('@'));
		}
		return "방문자";
	}

}
