package com.jieuni.common;

import org.springframework.util.StringUtils;

import java.util.Set;

// 프론트의 공개 페이지 캐시를 태그로 비운다. 테스트는 네트워크 없이 가짜 구현으로 바꾼다.
public interface RevalidateClient {

	// 실패해도 던지지 않는다. 공개 페이지는 1시간 주기로도 다시 만들어진다.
	void revalidate(Set<String> tags);

	// 재생성을 부를 주소. compose가 빈 REVALIDATE_URL을 넘겨도 FRONTEND_URL로 가도록 빈 문자열을 없는 값으로 본다. 시작 검사도 이 규칙을 쓴다.
	static String url(String revalidateUrl, String frontendUrl) {
		return StringUtils.hasText(revalidateUrl) ? revalidateUrl : frontendUrl;
	}

}
