package com.jieuni.admin;

import com.jieuni.IntegrationTest;
import com.jieuni.place.PlaceStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

// 커밋 때 일어나는 일(커밋 뒤 파일 삭제, 커밋 때 검사하는 position 유니크)은 테스트 트랜잭션 안에서 보이지 않는다.
// 여기서는 요청이 실제로 커밋되게 하고 데이터를 직접 지운다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminPhotoCommitTest extends IntegrationTest {

	private static final String ADMIN = "admin@test.jieuni";

	private Cookie admin;

	@BeforeEach
	void loginAsAdmin() {
		admin = devLogin(ADMIN);
	}

	@AfterEach
	void cleanUp() {
		var jdbc = data.jdbc();
		jdbc.sql("delete from place_photos where place_id in (select id from places where slug like 'commit-%')").update();
		jdbc.sql("delete from place_credits where place_id in (select id from places where slug like 'commit-%')").update();
		jdbc.sql("delete from places where slug like 'commit-%'").update();
		jdbc.sql("delete from makers where slug like 'commit-%'").update();
		jdbc.sql("delete from users where provider_id = :email").param("email", ADMIN).update();
	}

	private MvcTestResult delete(long place, long photo) {
		return mvc.delete().uri("/api/admin/places/" + place + "/photos/" + photo).cookie(admin).with(xsrf()).exchange();
	}

	private List<Integer> positions(long place) {
		return data.jdbc().sql("select position from place_photos where place_id = :id order by position")
				.param("id", place).query(Integer.class).list();
	}

	@Test
	void filesAreDeletedAfterCommit() {
		long place = data.place("commit-photo", PlaceStatus.DRAFT, null);
		String key = "uploads/" + place + "/a";
		long photo = data.photo(place, 0, key);
		storage.putPhoto(key, DEFAULT_NOW);

		assertThat(delete(place, photo)).hasStatus(HttpStatus.NO_CONTENT);

		assertThat(storage.deleted()).containsExactlyInAnyOrder(key + "-1600.webp", key + "-800.webp");
		assertThat(storage.exists(key + "-1600.webp")).isFalse();
	}

	@Test
	void rejectedDeleteKeepsRowAndFiles() {
		long place = data.publishablePlace("commit-published", PlaceStatus.PUBLISHED,
				Instant.parse("2026-09-01T00:00:00Z"));
		long photo = data.jdbc().sql("select id from place_photos where place_id = :id").param("id", place)
				.query(Long.class).single();
		String key = data.jdbc().sql("select object_key from place_photos where id = :id").param("id", photo)
				.query(String.class).single();
		storage.putPhoto(key, DEFAULT_NOW);

		assertThat(delete(place, photo)).hasStatus(HttpStatus.BAD_REQUEST);

		assertThat(positions(place)).containsExactly(0);
		assertThat(storage.deleted()).isEmpty();
		assertThat(storage.exists(key + "-1600.webp")).isTrue();
	}

	@Test
	void concurrentAddsTakeNextPositions() throws Exception {
		long place = data.place("commit-concurrent", PlaceStatus.DRAFT, null);
		List<String> keys = List.of("uploads/" + place + "/" + UUID.randomUUID(), "uploads/" + place + "/" + UUID.randomUUID());
		keys.forEach(key -> storage.putPhoto(key, DEFAULT_NOW));

		CountDownLatch start = new CountDownLatch(1);
		List<MvcTestResult> results;
		try (var pool = Executors.newFixedThreadPool(keys.size())) {
			List<Future<MvcTestResult>> futures = keys.stream().map(key -> pool.submit(() -> {
				start.await();
				return mvc.post().uri("/api/admin/places/" + place + "/photos").cookie(admin).with(xsrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"objectKey":"%s","width":1600,"height":1067,"license":"own"}""".formatted(key))
						.exchange();
			})).toList();
			start.countDown();
			results = futures.stream().map(f -> {
				try {
					return f.get();
				} catch (Exception e) {
					throw new IllegalStateException(e);
				}
			}).toList();
		}

		results.forEach(result -> assertThat(result).hasStatus(HttpStatus.CREATED));
		assertThat(positions(place)).containsExactly(0, 1);
	}

}
