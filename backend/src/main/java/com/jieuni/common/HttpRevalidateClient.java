package com.jieuni.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

// POST {주소}/api/revalidate. 커밋 직후 관리자 요청 스레드에서 부르므로 오래 기다리지 않는다.
@Component
class HttpRevalidateClient implements RevalidateClient {

	private static final Logger log = LoggerFactory.getLogger(HttpRevalidateClient.class);
	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

	// 주소나 비밀이 없으면 null이고 호출을 건너뛴다.
	private final RestClient http;

	@Autowired
	HttpRevalidateClient(@Value("${REVALIDATE_URL:}") String revalidateUrl, @Value("${FRONTEND_URL:}") String frontendUrl,
			@Value("${jieuni.revalidate.secret:}") String secret) {
		this(RestClient.builder().requestFactory(requestFactory()), RevalidateClient.url(revalidateUrl, frontendUrl), secret);
	}

	HttpRevalidateClient(RestClient.Builder builder, String url, String secret) {
		if (StringUtils.hasText(url) && StringUtils.hasText(secret)) {
			this.http = builder.baseUrl(url).defaultHeader("X-Revalidate-Secret", secret).build();
		} else {
			// 운영은 시작할 때 막는다. 개발에서 .env 없이 띄우면 호출만 건너뛴다.
			log.warn("재생성 주소나 비밀이 없어 재생성 호출을 건너뛴다");
			this.http = null;
		}
	}

	@Override
	public void revalidate(Set<String> tags) {
		if (http == null || tags.isEmpty()) {
			return;
		}
		try {
			http.post().uri("/api/revalidate").contentType(MediaType.APPLICATION_JSON)
					.body(Map.of("tags", tags)).retrieve().toBodilessEntity();
		} catch (RestClientException e) {
			log.warn("재생성 호출 실패: {}", tags, e);
		}
	}

	private static JdkClientHttpRequestFactory requestFactory() {
		// JDK 클라이언트는 http://에도 h2c 업그레이드를 붙이는데, Next 서버는 그 요청을 응답 없이 끊는다.
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1).connectTimeout(CONNECT_TIMEOUT).build());
		factory.setReadTimeout(READ_TIMEOUT);
		return factory;
	}

}
