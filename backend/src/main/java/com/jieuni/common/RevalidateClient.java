package com.jieuni.common;

import java.util.Set;

// 프론트의 공개 페이지 캐시를 태그로 비운다. 테스트는 네트워크 없이 가짜 구현으로 바꾼다.
public interface RevalidateClient {

	// 실패해도 던지지 않는다. 공개 페이지는 1시간 주기로도 다시 만들어진다.
	void revalidate(Set<String> tags);

}
