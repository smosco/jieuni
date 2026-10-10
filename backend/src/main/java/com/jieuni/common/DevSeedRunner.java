package com.jieuni.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 개발용 가짜 데이터(seed/seed.json)를 makers가 비어 있을 때만 한 트랜잭션으로 넣는다.
// 행을 직접 써서 공개 시 검증(PublishRules)을 거치지 않는다. 공개 페이지가 있는 공간이 그 검증을 통과하는지는 테스트가 본다
// (통과하지 못하면 관리자 폼에서 어떤 값도 저장할 수 없다).
@Component
@Profile("dev")
class DevSeedRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DevSeedRunner.class);
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");
	private static final Set<String> PUBLISHED_ONCE = Set.of("published", "paused", "closed");

	private final JdbcClient jdbc;
	private final TransactionTemplate tx;
	private final JsonMapper json;
	private final Clock clock;
	private final Path seedPath;

	DevSeedRunner(JdbcClient jdbc, TransactionTemplate tx, JsonMapper json, Clock clock,
			@Value("${jieuni.seed-path}") Path seedPath) {
		this.jdbc = jdbc;
		this.tx = tx;
		this.json = json;
		this.clock = clock;
		this.seedPath = seedPath;
	}

	@Override
	public void run(ApplicationArguments args) {
		boolean seeded = jdbc.sql("select exists (select 1 from makers)").query(Boolean.class).single();
		if (seeded) {
			log.info("개발용 시드: makers에 행이 있어 건너뛴다");
			return;
		}
		Seed seed = json.readValue(seedPath.toFile(), Seed.class);
		tx.executeWithoutResult(status -> load(seed));
		log.info("개발용 시드: 만든 사람 {}, 공간 {}, 편성 {} 적재 ({})",
				seed.makers().size(), seed.places().size(), seed.picks().size(), seedPath);
	}

	private void load(Seed seed) {
		// pgjdbc는 Instant 파라미터를 못 받으므로 OffsetDateTime(UTC)으로 넘긴다.
		OffsetDateTime now = clock.instant().atOffset(ZoneOffset.UTC);
		Map<String, Long> makerIds = new HashMap<>();
		Map<String, Long> placeIds = new HashMap<>();

		for (MakerSeed m : seed.makers()) {
			long id = jdbc.sql("""
					insert into makers (slug, name, kind, region, intro, website, instagram, created_at, updated_at)
					values (:slug, :name, :kind, :region, :intro, :website, :instagram, :now, :now)
					returning id""")
					.param("slug", m.slug()).param("name", m.name()).param("kind", m.kind())
					.param("region", m.region()).param("intro", m.intro())
					.param("website", m.website()).param("instagram", m.instagram())
					.param("now", now)
					.query(Long.class).single();
			makerIds.put(m.slug(), id);
		}
		for (MakerSeed m : seed.makers()) {
			if (m.affiliation() != null) {
				jdbc.sql("update makers set affiliation_id = :affiliation where id = :id")
						.param("affiliation", makerIds.get(m.affiliation())).param("id", makerIds.get(m.slug()))
						.update();
			}
		}

		for (PlaceSeed p : seed.places()) {
			OffsetDateTime publishedAt = PUBLISHED_ONCE.contains(p.status()) ? now.minusDays(p.publishedDaysAgo()) : null;
			long placeId = jdbc.sql("""
					insert into places (slug, name, kind, region, address, lat, lng, access, visit_note, summary, impression,
					                    sense_light, sense_sound, sense_scent, sense_touch, essay, visited_by_editor_on,
					                    status, published_at, created_at, updated_at)
					values (:slug, :name, :kind, :region, :address, :lat, :lng, :access, :visitNote, :summary, :impression,
					        :light, :sound, :scent, :touch, :essay, :visitedByEditorOn,
					        :status, :publishedAt, :now, :now)
					returning id""")
					.param("slug", p.slug()).param("name", p.name()).param("kind", p.kind())
					.param("region", p.region()).param("address", p.address())
					.param("lat", p.lat()).param("lng", p.lng()).param("access", p.access())
					.param("visitNote", p.visitNote()).param("summary", p.summary()).param("impression", p.impression())
					.param("light", p.senses().light()).param("sound", p.senses().sound())
					.param("scent", p.senses().scent()).param("touch", p.senses().touch())
					.param("essay", p.essay()).param("visitedByEditorOn", p.visitedByEditorOn())
					.param("status", p.status()).param("publishedAt", publishedAt).param("now", now)
					.query(Long.class).single();
			placeIds.put(p.slug(), placeId);

			for (int i = 0; i < p.credits().size(); i++) {
				CreditSeed c = p.credits().get(i);
				jdbc.sql("""
						insert into place_credits (place_id, maker_id, role, verified, source_url, position)
						values (:placeId, :makerId, :role, :verified, :sourceUrl, :position)""")
						.param("placeId", placeId).param("makerId", makerIds.get(c.maker()))
						.param("role", c.role()).param("verified", c.verified())
						.param("sourceUrl", c.sourceUrl()).param("position", i)
						.update();
			}
			for (int i = 0; i < p.embeds().size(); i++) {
				EmbedSeed e = p.embeds().get(i);
				jdbc.sql("""
						insert into place_embeds (place_id, type, url, position)
						values (:placeId, :type, :url, :position)""")
						.param("placeId", placeId).param("type", e.type()).param("url", e.url()).param("position", i)
						.update();
			}
			for (int i = 0; i < p.sources().size(); i++) {
				SourceSeed s = p.sources().get(i);
				jdbc.sql("""
						insert into place_sources (place_id, title, url, type, position)
						values (:placeId, :title, :url, :type, :position)""")
						.param("placeId", placeId).param("title", s.title()).param("url", s.url())
						.param("type", s.type()).param("position", i)
						.update();
			}
		}

		// 편성은 "몇 주 전 무슨 요일"로 적혀 있어서, 언제 적재해도 이번 주·지난주 칸에 들어간다.
		LocalDate thisMonday = LocalDate.ofInstant(clock.instant(), KST).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		for (PickSeed pick : seed.picks()) {
			LocalDate date = thisMonday.minusWeeks(pick.weeksAgo()).with(TemporalAdjusters.nextOrSame(pick.weekday()));
			jdbc.sql("""
					insert into daily_picks (pick_date, place_id, line, created_at, updated_at)
					values (:date, :placeId, :line, :now, :now)""")
					.param("date", date).param("placeId", placeIds.get(pick.place()))
					.param("line", pick.line()).param("now", now)
					.update();
		}
	}

	record Seed(List<MakerSeed> makers, List<PlaceSeed> places, List<PickSeed> picks) {
	}

	record MakerSeed(String slug, String name, String kind, String region, String intro, String website,
			String instagram, String affiliation) {
	}

	record PlaceSeed(String slug, String name, String kind, String region, String address, BigDecimal lat,
			BigDecimal lng, String access, String visitNote, String summary, String impression, Senses senses,
			String essay, LocalDate visitedByEditorOn, String status, int publishedDaysAgo, List<CreditSeed> credits,
			List<SourceSeed> sources, List<EmbedSeed> embeds) {
	}

	record Senses(String light, String sound, String scent, String touch) {
	}

	record CreditSeed(String maker, String role, boolean verified, String sourceUrl) {
	}

	record SourceSeed(String title, String url, String type) {
	}

	record EmbedSeed(String type, String url) {
	}

	record PickSeed(int weeksAgo, DayOfWeek weekday, String place, String line) {
	}

}
