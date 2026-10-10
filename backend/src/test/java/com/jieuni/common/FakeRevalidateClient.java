package com.jieuni.common;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

// 재생성 호출 한 번이 태그 집합 하나다. 테스트가 보낸 태그와 횟수를 확인한다.
public class FakeRevalidateClient implements RevalidateClient {

	private final List<Set<String>> calls = new CopyOnWriteArrayList<>();

	public void reset() {
		calls.clear();
	}

	public List<Set<String>> calls() {
		return calls;
	}

	@Override
	public void revalidate(Set<String> tags) {
		calls.add(Set.copyOf(tags));
	}

}
