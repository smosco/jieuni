package com.jieuni.upload;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 버킷 대신 메모리 맵. 테스트가 파일을 놓고, 지운 키를 확인한다.
public class FakeObjectStorage implements ObjectStorage {

	private final Map<String, Instant> objects = new ConcurrentHashMap<>();
	private final List<String> deleted = new CopyOnWriteArrayList<>();

	public void reset() {
		objects.clear();
		deleted.clear();
	}

	public void put(String key, Instant lastModified) {
		objects.put(key, lastModified);
	}

	// 사진 한 장의 두 크기를 놓는다.
	public void putPhoto(String objectKey, Instant lastModified) {
		PhotoFiles.files(objectKey).forEach(file -> put(file, lastModified));
	}

	public List<String> deleted() {
		return deleted;
	}

	@Override
	public List<StoredObject> list(String prefix) {
		return objects.entrySet().stream().filter(e -> e.getKey().startsWith(prefix))
				.map(e -> new StoredObject(e.getKey(), e.getValue())).toList();
	}

	@Override
	public boolean exists(String key) {
		return objects.containsKey(key);
	}

	@Override
	public void delete(Collection<String> keys) {
		keys.forEach(objects::remove);
		deleted.addAll(keys);
	}

}
