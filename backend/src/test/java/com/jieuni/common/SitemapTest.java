package com.jieuni.common;

import com.jieuni.IntegrationTest;
import com.jieuni.place.CreditRole;
import com.jieuni.place.PlaceStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SitemapTest extends IntegrationTest {

	@Test
	void listsEveryPublicPageAndRunsTwoQueries() {
		Instant at = Instant.parse("2026-10-01T00:00:00Z");
		long withPage = data.maker("with-page");
		long draftOnly = data.maker("draft-only");
		data.credit(data.place("published", PlaceStatus.PUBLISHED, at), withPage, CreditRole.INTERIOR, 0);
		data.place("paused", PlaceStatus.PAUSED, at);
		data.place("closed", PlaceStatus.CLOSED, at);
		data.credit(data.place("draft", PlaceStatus.DRAFT, null), draftOnly, CreditRole.INTERIOR, 0);
		data.place("suggested", PlaceStatus.SUGGESTED, null);

		statistics().clear();
		var json = assertThat(mvc.get().uri("/api/sitemap")).hasStatusOk().bodyJson();
		json.extractingPath("$.places[*].slug").asArray().containsExactly("closed", "paused", "published");
		json.extractingPath("$.makers[*].slug").asArray().containsExactly("with-page");
		json.extractingPath("$.places[0].updatedAt").isEqualTo("2026-01-01T00:00:00Z");
		assertThat(statistics().getPrepareStatementCount()).isEqualTo(2);
	}

}
