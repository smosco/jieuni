package com.jieuni;

import com.jieuni.follow.Follow;
import com.jieuni.follow.FollowRepository;
import com.jieuni.maker.Maker;
import com.jieuni.maker.MakerKind;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Transactional
class SchemaTest {

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	EntityManager em;

	@Autowired
	FollowRepository followRepository;

	@Test
	void flywayMadeTwelveTablesPlusTwoSessionTables() {
		Integer tables = jdbc.queryForObject(
				"select count(*) from information_schema.tables where table_schema = 'public' and table_name <> 'flyway_schema_history'",
				Integer.class);
		Integer migrations = jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class);
		assertThat(tables).isEqualTo(14); // SPEC 3절 12개 + Spring Session 2개
		assertThat(migrations).isEqualTo(2);
	}

	@Test
	void enumIsStoredAsLowercaseValueAndAuditingFillsTimestamps() {
		Maker maker = new Maker("이로재", MakerKind.STUDIO);
		em.persist(maker);
		em.flush();

		String kind = jdbc.queryForObject("select kind from makers where id = ?", String.class, maker.getId());
		Instant createdAt = jdbc.queryForObject("select created_at from makers where id = ?", Instant.class, maker.getId());
		assertThat(kind).isEqualTo("studio");
		assertThat(createdAt).isNotNull();
		assertThat(maker.getUpdatedAt()).isEqualTo(maker.getCreatedAt());
	}

	@Test
	void savingNewFollowInsertsWithoutSelect() {
		OffsetDateTime now = OffsetDateTime.of(2026, 10, 6, 0, 0, 0, 0, ZoneOffset.UTC); // pgjdbc는 Instant 파라미터를 못 받는다
		jdbc.update("insert into users (id, provider, provider_id, nickname, role, created_at, updated_at) values (1, 'dev', 'u1', '지은이', 'user', ?, ?)", now, now);
		jdbc.update("insert into makers (id, name, kind, created_at, updated_at) values (1, '이로재', 'studio', ?, ?)", now, now);

		Statistics stats = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
		stats.clear();

		followRepository.save(new Follow(1L, 1L));
		em.flush();

		assertThat(stats.getEntityLoadCount()).isZero();
		assertThat(stats.getPrepareStatementCount()).isEqualTo(1); // INSERT 하나
	}

}
