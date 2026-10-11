package com.jieuni.common;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
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
	private final List<String> received = new CopyOnWriteArrayList<>();
	private HttpServer local;
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

	// 운영 compose는 비운 REVALIDATE_URL을 빈 문자열로 넘긴다. 빈 주입 경로 그대로 띄워 실제로 어디로 가는지 본다.
	@Test
	void blankRevalidateUrlFallsBackToFrontendUrl() throws IOException {
		assertThat(hostsCalled("REVALIDATE_URL=", "FRONTEND_URL=" + frontend())).containsExactly("frontend");
	}

	@Test
	void missingRevalidateUrlFallsBackToFrontendUrl() throws IOException {
		assertThat(hostsCalled("FRONTEND_URL=" + frontend())).containsExactly("frontend");
	}

	@Test
	void revalidateUrlWinsOverFrontendUrl() throws IOException {
		assertThat(hostsCalled("REVALIDATE_URL=" + frontend(), "FRONTEND_URL=http://public.invalid")).containsExactly("frontend");
	}

	@Test
	void skipsWhenSecretIsMissing() {
		server.expect(never(), requestTo("http://frontend:3000/api/revalidate"));

		new HttpRevalidateClient(builder, "http://frontend:3000", "").revalidate(tags());

		server.verify();
	}

	@AfterEach
	void stopLocal() {
		if (local != null) {
			local.stop(0);
		}
	}

	// 받은 재생성 요청을 기록하는 프론트 대역. 주소는 http://127.0.0.1:<포트>.
	private String frontend() throws IOException {
		if (local == null) {
			local = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
			local.createContext("/api/revalidate", exchange -> {
				received.add("frontend");
				exchange.sendResponseHeaders(200, -1);
				exchange.close();
			});
			local.start();
		}
		return "http://127.0.0.1:" + local.getAddress().getPort();
	}

	private List<String> hostsCalled(String... properties) {
		new ApplicationContextRunner().withBean(HttpRevalidateClient.class)
				.withPropertyValues(properties)
				.withPropertyValues("jieuni.revalidate.secret=s3cret")
				.run(context -> context.getBean(RevalidateClient.class).revalidate(tags()));
		return received;
	}

}
