package com.jieuni;

import com.jieuni.maker.MakerKind;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceKind;
import com.jieuni.place.PlaceStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

// 테스트가 필요한 행을 직접 만든다(시드에 기대지 않는다). 테스트 트랜잭션 안에서 써서 함께 롤백된다.
public class TestData {

	private static final OffsetDateTime CREATED = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

	private final JdbcClient jdbc;

	TestData(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public JdbcClient jdbc() {
		return jdbc;
	}

	public long maker(String slug) {
		return maker(slug, MakerKind.PERSON);
	}

	public long maker(String slug, MakerKind kind) {
		return jdbc.sql("""
				insert into makers (slug, name, kind, created_at, updated_at)
				values (:slug, :name, :kind, :at, :at) returning id""")
				.param("slug", slug).param("name", slug + " 이름").param("kind", kind.value()).param("at", CREATED)
				.query(Long.class).single();
	}

	// published·paused·closed는 publishedAt이 있어야 한다. 나머지는 null을 넘긴다.
	public long place(String slug, PlaceStatus status, Instant publishedAt) {
		return place(slug, status, publishedAt, PlaceKind.GARDEN);
	}

	public long place(String slug, PlaceStatus status, Instant publishedAt, PlaceKind kind) {
		return jdbc.sql("""
				insert into places (slug, name, kind, region, status, published_at, created_at, updated_at)
				values (:slug, :name, :kind, '서울 종로구', :status, :publishedAt, :at, :at) returning id""")
				.param("slug", slug).param("name", slug + " 이름").param("kind", kind.value())
				.param("status", status.value())
				.param("publishedAt", publishedAt == null ? null : publishedAt.atOffset(ZoneOffset.UTC))
				.param("at", CREATED)
				.query(Long.class).single();
	}

	public void credit(long placeId, long makerId, CreditRole role, int position) {
		jdbc.sql("""
				insert into place_credits (place_id, maker_id, role, verified, source_url, position)
				values (:placeId, :makerId, :role, true, 'https://example.com/credit', :position)""")
				.param("placeId", placeId).param("makerId", makerId).param("role", role.value())
				.param("position", position)
				.update();
	}

	public void pick(LocalDate date, long placeId) {
		jdbc.sql("""
				insert into daily_picks (pick_date, place_id, line, created_at, updated_at)
				values (:date, :placeId, :line, :at, :at)""")
				.param("date", date).param("placeId", placeId).param("line", date + "의 한 줄").param("at", CREATED)
				.update();
	}

	public void photo(long placeId, int position) {
		jdbc.sql("""
				insert into place_photos (place_id, object_key, width, height, alt, credit, license, position)
				values (:placeId, :key, 1600, 1067, '사진 설명', '에디터', 'own', :position)""")
				.param("placeId", placeId).param("key", "places/" + placeId + "/p" + position)
				.param("position", position)
				.update();
	}

}
