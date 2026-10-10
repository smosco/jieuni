package com.jieuni.upload;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

// 버킷 목록·확인·삭제. 테스트는 네트워크 없이 가짜 구현으로 바꾼다.
public interface ObjectStorage {

	List<StoredObject> list(String prefix);

	boolean exists(String key);

	void delete(Collection<String> keys);

	record StoredObject(String key, Instant lastModified) {
	}

}
