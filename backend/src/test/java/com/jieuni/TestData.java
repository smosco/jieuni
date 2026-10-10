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

	// 공개 시 필수를 모두 채운 공간: 출처 있는 크레딧 1(만든 사람 "{slug}-maker"), 사진 1, 머물러 본 날이 있어 출처는 없어도 된다.
	public long publishablePlace(String slug, PlaceStatus status, Instant publishedAt) {
		long place = place(slug, status, publishedAt);
		jdbc.sql("""
				update places set address = '서울 종로구 1', lat = 37.5, lng = 127.0, access = 'ticket',
				  summary = '한 문장', impression = '감상', visited_by_editor_on = '2026-05-01'
				where id = :id""").param("id", place).update();
		credit(place, maker(slug + "-maker"), CreditRole.ARCHITECTURE, 0);
		photo(place, 0);
		return place;
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
		photo(placeId, position, "places/" + placeId + "/p" + position);
	}

	public long photo(long placeId, int position, String objectKey) {
		return jdbc.sql("""
				insert into place_photos (place_id, object_key, width, height, alt, credit, license, position)
				values (:placeId, :key, 1600, 1067, '사진 설명', '에디터', 'own', :position) returning id""")
				.param("placeId", placeId).param("key", objectKey)
				.param("position", position)
				.query(Long.class).single();
	}

	public long userId(String email) {
		return jdbc.sql("select id from users where provider_id = :email").param("email", email).query(Long.class).single();
	}

	// 사용자가 보낸 공간 제보(제보한 사람이 없으면 탈퇴한 것).
	public long suggestedPlace(String name, Long userId, Instant at) {
		return jdbc.sql("""
				insert into places (name, region, status, suggested_by, suggestion_reason, suggestion_link,
				  suggestion_maker_hint, created_at, updated_at)
				values (:name, '서울', 'suggested', :userId, '오래 생각나요', 'https://example.com/s', '건축가 누구', :at, :at)
				returning id""")
				.param("name", name).param("userId", userId).param("at", at.atOffset(ZoneOffset.UTC))
				.query(Long.class).single();
	}

	public long creditSuggestion(long placeId, Long userId, CreditRole role, String sourceUrl, Instant at) {
		return jdbc.sql("""
				insert into credit_suggestions (user_id, place_id, role, maker_name, source_url, is_self, status, created_at)
				values (:userId, :placeId, :role, '제보된 이름', :sourceUrl, false, 'pending', :at) returning id""")
				.param("userId", userId).param("placeId", placeId).param("role", role.value())
				.param("sourceUrl", sourceUrl).param("at", at.atOffset(ZoneOffset.UTC))
				.query(Long.class).single();
	}

	public void llmCall(String costUsd, Instant at) {
		jdbc.sql("""
				insert into llm_calls (purpose, model, input_tokens, output_tokens, cost_usd, latency_ms, success, created_at)
				values ('draft', 'test-model', 10, 10, cast(:cost as numeric), 100, true, :at)""")
				.param("cost", costUsd).param("at", at.atOffset(ZoneOffset.UTC)).update();
	}

}
