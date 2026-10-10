package com.jieuni.upload;

import com.jieuni.place.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

// 브라우저가 올리고 등록하지 않은 파일을 지운다. 등록 여부는 place_photos가 판단한다.
@Component
class OrphanUploadCleaner {

	private static final Logger log = LoggerFactory.getLogger(OrphanUploadCleaner.class);
	// 업로드와 등록 사이에 지우지 않게 하루를 기다린다.
	private static final Duration GRACE = Duration.ofHours(24);

	private final ObjectStorage storage;
	private final PlaceRepository places;
	private final Clock clock;

	OrphanUploadCleaner(ObjectStorage storage, PlaceRepository places, Clock clock) {
		this.storage = storage;
		this.places = places;
		this.clock = clock;
	}

	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	void scheduled() {
		try {
			cleanUp();
		} catch (RuntimeException e) {
			log.error("고아 사진 정리 실패", e);
		}
	}

	int cleanUp() {
		Instant cutoff = clock.instant().minus(GRACE);
		List<ObjectStorage.StoredObject> old = storage.list(PhotoFiles.PREFIX).stream()
				.filter(o -> !o.lastModified().isAfter(cutoff))
				.toList();
		if (old.isEmpty()) {
			return 0;
		}
		Set<String> keys = new HashSet<>();
		old.forEach(o -> keys.add(PhotoFiles.keyOf(o.key())));
		keys.remove(null);
		Set<String> registered = keys.isEmpty() ? Set.of() : places.findRegisteredObjectKeys(keys);
		List<String> orphans = old.stream()
				.map(ObjectStorage.StoredObject::key)
				.filter(file -> !registered.contains(Objects.requireNonNullElse(PhotoFiles.keyOf(file), "")))
				.toList();
		if (!orphans.isEmpty()) {
			storage.delete(orphans);
		}
		log.info("고아 사진 정리: 파일 {}개 삭제", orphans.size());
		return orphans.size();
	}

}
