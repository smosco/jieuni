package com.jieuni.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

// 프론트 라우트(app/api/revalidate/route.ts)가 받는 모양으로 보내는지, 실패해도 던지지 않는지.
class HttpRevalidateClientTest {

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

	private static LinkedHashSet<String> tags() {
		return new LinkedHashSet<>(List.of("place:garden", "browse"));
	}

	@Test
	void postsTagsWithSecret() {
		server.expect(requestTo("http://frontend:3000/api/revalidate"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("X-Revalidate-Secret", "s3cret"))
				.andExpect(content().json("{\"tags\":[\"place:garden\",\"browse\"]}", JsonCompareMode.STRICT))
				.andRespond(withSuccess());

		new HttpRevalidateClient(builder, "http://frontend:3000", "s3cret").revalidate(tags());

		server.verify();
	}

	@Test
	void rejectedOrUnreachableFrontendDoesNotThrow() {
		server.expect(requestTo("http://frontend:3000/api/revalidate")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
		server.expect(requestTo("http://frontend:3000/api/revalidate")).andRespond(withException(new IOException("연결 거부")));
		HttpRevalidateClient client = new HttpRevalidateClient(builder, "http://frontend:3000", "s3cret");

		assertThatNoException().isThrownBy(() -> client.revalidate(tags()));
		assertThatNoException().isThrownBy(() -> client.revalidate(tags()));

		server.verify();
	}

	@Test
	void skipsWhenSecretIsMissing() {
		server.expect(never(), requestTo("http://frontend:3000/api/revalidate"));

		new HttpRevalidateClient(builder, "http://frontend:3000", "").revalidate(tags());

		server.verify();
	}

}
