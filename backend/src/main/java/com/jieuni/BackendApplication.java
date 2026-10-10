package com.jieuni;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(BackendApplication.class);
		// 설정 바인딩은 못 푼 ${...}를 문자열 그대로 넘기므로, prod에서는 빈을 만들기 전에 없거나 비어 있으면 멈춘다.
		app.addInitializers(context -> {
			var env = context.getEnvironment();
			if (env.matchesProfiles("prod")) {
				var missing = missingProdEnv(env);
				if (!missing.isEmpty()) {
					throw new IllegalStateException("prod 필수 환경 변수가 없거나 비어 있다: " + missing);
				}
			}
		});
		app.run(args);
	}

	static List<String> missingProdEnv(Environment env) {
		List<String> missing = new ArrayList<>(Stream.of("DB_URL", "POSTGRES_USER", "POSTGRES_PASSWORD",
				"PHOTO_PUBLIC_BASE_URL", "S3_ENDPOINT", "S3_BUCKET", "S3_ACCESS_KEY", "S3_SECRET_KEY",
				"REVALIDATE_SECRET")
				.filter(name -> !StringUtils.hasText(env.getProperty(name)))
				.toList());
		// 재생성 주소는 따로 주지 않으면 공개 주소를 쓴다.
		if (!StringUtils.hasText(env.getProperty("REVALIDATE_URL"))
				&& !StringUtils.hasText(env.getProperty("FRONTEND_URL"))) {
			missing.add("REVALIDATE_URL 또는 FRONTEND_URL");
		}
		return missing;
	}

}
