package com.jieuni.user;

import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

// 세션 저장소는 요청마다 따로(REQUIRES_NEW) 커밋하므로, 세션을 지우는 탈퇴는 테스트 트랜잭션 없이 실제로 커밋해서 본다.
// 만든 데이터는 직접 지운다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class WithdrawApiTest extends IntegrationTest {

	private static final String EMAIL = "withdraw@test.jieuni";

	private static final String OTHER = "withdraw-other@test.jieuni";

	// 사용자를 먼저 지운다(저장·팔로우는 FK로 함께 지워지고, 그래야 공간·만든 사람을 지울 수 있다).
	@AfterEach
	void cleanUp() {
		var jdbc = data.jdbc();
		jdbc.sql("delete from spring_session where principal_name in "
				+ "(select id::text from users where provider_id in (:a, :b))").param("a", EMAIL).param("b", OTHER).update();
		jdbc.sql("delete from users where provider_id in (:a, :b)").param("a", EMAIL).param("b", OTHER).update();
		jdbc.sql("delete from credit_suggestions where place_id in (select id from places where slug = 'withdraw-place')")
				.update();
		jdbc.sql("delete from places where slug = 'withdraw-place' or name = '탈퇴 전 제보'").update();
		jdbc.sql("delete from makers where slug = 'withdraw-maker'").update();
	}

	@Test
	void withdrawDeletesUserDataKeepsSuggestionsAndEndsEverySession() {
		Cookie first = devLogin(EMAIL);
		Cookie second = devLogin(EMAIL);
		Cookie other = devLogin(OTHER);
		long userId = data.userId(EMAIL);
		long otherId = data.userId(OTHER);
		long placeId = data.place("withdraw-place", PlaceStatus.PUBLISHED, clock.instant());
		long makerId = data.maker("withdraw-maker");
		var jdbc = data.jdbc();
		for (long id : new long[] {userId, otherId}) {
			jdbc.sql("insert into saves (user_id, place_id, status, updated_at) values (:u, :p, 'want', now())")
					.param("u", id).param("p", placeId).update();
			jdbc.sql("insert into follows (user_id, maker_id, created_at) values (:u, :m, now())")
					.param("u", id).param("m", makerId).update();
		}
		long suggestedPlace = data.suggestedPlace("탈퇴 전 제보", userId, clock.instant());
		long creditSuggestion = data.creditSuggestion(placeId, userId, CreditRole.ARCHITECTURE, null, clock.instant());
		assertThat(sessionsOf(userId)).isEqualTo(2);

		MvcTestResult result = mvc.delete().uri("/api/me").cookie(first).with(xsrf()).exchange();

		assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
		Cookie cleared = result.getResponse().getCookie("SESSION");
		assertThat(cleared).isNotNull();
		assertThat(cleared.getMaxAge()).isZero();
		assertThat(mvc.get().uri("/api/me").cookie(first)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.get().uri("/api/me").cookie(second)).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(sessionsOf(userId)).isZero();
		assertThat(count("select count(*) from users where id = :id", userId)).isZero();
		assertThat(count("select count(*) from saves where user_id = :id", userId)).isZero();
		assertThat(count("select count(*) from follows where user_id = :id", userId)).isZero();
		// 제보는 남고 작성자만 비운다.
		assertThat(count("select count(*) from places where id = :id and suggested_by is null", suggestedPlace)).isOne();
		assertThat(count("select count(*) from credit_suggestions where id = :id and user_id is null", creditSuggestion))
				.isOne();

		// 다른 사용자는 그대로다.
		assertThat(mvc.get().uri("/api/me").cookie(other)).hasStatusOk();
		assertThat(count("select count(*) from saves where user_id = :id", otherId)).isOne();
		assertThat(count("select count(*) from follows where user_id = :id", otherId)).isOne();
	}

	@Test
	void withdrawNeedsLoginAndCsrf() {
		Cookie session = devLogin(EMAIL);

		assertThat(mvc.delete().uri("/api/me").with(xsrf())).hasStatus(HttpStatus.UNAUTHORIZED);
		assertThat(mvc.delete().uri("/api/me").cookie(session)).hasStatus(HttpStatus.FORBIDDEN);
		assertThat(mvc.get().uri("/api/me").cookie(session)).hasStatusOk();
	}

	private long sessionsOf(long userId) {
		return data.jdbc().sql("select count(*) from spring_session where principal_name = :name")
				.param("name", String.valueOf(userId)).query(Long.class).single();
	}

	private long count(String sql, long id) {
		return data.jdbc().sql(sql).param("id", id).query(Long.class).single();
	}

}
