package com.jieuni.upload;

import com.jieuni.IntegrationTest;
import com.jieuni.place.PlaceStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class OrphanUploadCleanerTest extends IntegrationTest {

	@Autowired
	OrphanUploadCleaner cleaner;

	@Test
	void deletesOnlyUnregisteredFilesOlderThanADay() {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		var old = DEFAULT_NOW.minus(Duration.ofHours(25));
		var exactlyADay = DEFAULT_NOW.minus(Duration.ofHours(24));
		var fresh = DEFAULT_NOW.minus(Duration.ofHours(1));

		String registered = "uploads/" + place + "/registered";
		data.photo(place, 0, registered);
		storage.putPhoto(registered, old);
		storage.putPhoto("uploads/" + place + "/orphan", old);
		storage.putPhoto("uploads/" + place + "/boundary", exactlyADay);
		storage.putPhoto("uploads/" + place + "/fresh", fresh);
		storage.put("uploads/" + place + "/odd.png", old);
		// uploads/ 밖은 보지 않는다(시드·백업 등).
		storage.put("backups/db.sql", old);

		assertThat(cleaner.cleanUp()).isEqualTo(5);

		assertThat(storage.deleted()).containsExactlyInAnyOrder(
				"uploads/" + place + "/orphan-1600.webp", "uploads/" + place + "/orphan-800.webp",
				"uploads/" + place + "/boundary-1600.webp", "uploads/" + place + "/boundary-800.webp",
				"uploads/" + place + "/odd.png");
		assertThat(storage.exists(registered + "-1600.webp")).isTrue();
		assertThat(storage.exists("uploads/" + place + "/fresh-800.webp")).isTrue();
		assertThat(storage.exists("backups/db.sql")).isTrue();
	}

	@Test
	void nothingToDo() {
		assertThat(cleaner.cleanUp()).isZero();
		assertThat(storage.deleted()).isEmpty();
	}

}
