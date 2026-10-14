package com.jieuni.upload;

import com.jayway.jsonpath.JsonPath;
import com.jieuni.IntegrationTest;
import com.jieuni.place.PlaceStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import static org.assertj.core.api.Assertions.assertThat;

// 서명은 네트워크 없이 실제 S3Presigner로 만든다(application-test.yml의 가짜 엔드포인트·키).
class UploadApiTest extends IntegrationTest {

	private MvcTestResult issue(Cookie who, String body) {
		return mvc.post().uri("/api/admin/uploads").cookie(who).with(xsrf())
				.contentType(MediaType.APPLICATION_JSON).content(body).exchange();
	}

	private static String body(Object placeId, String contentType, long w1600, long w800) {
		return "{\"placeId\":%s,\"contentType\":\"%s\",\"sizes\":{\"w1600\":%d,\"w800\":%d}}"
				.formatted(placeId, contentType, w1600, w800);
	}

	@Test
	void issuesTwoPutUrlsForWebp() throws Exception {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		MvcTestResult result = issue(devLogin("admin@test.jieuni"), body(place, "image/webp", 300_000, 90_000));
		assertThat(result).hasStatusOk();

		String body = result.getResponse().getContentAsString();
		String key = JsonPath.read(body, "$.objectKey");
		assertThat(PhotoFiles.belongsTo(key, place)).isTrue();
		for (String size : new String[] {"1600", "800"}) {
			String url = JsonPath.read(body, "$.uploadUrls.w" + size);
			// 버킷을 경로에 둔 R2 API 주소. 공개 주소(r2.dev·커스텀 도메인)로는 서명 URL이 동작하지 않는다.
			assertThat(url).startsWith("https://test.r2.cloudflarestorage.com/jieuni-test/" + key + "-" + size + ".webp?");
			assertThat(url).contains("X-Amz-Algorithm=AWS4-HMAC-SHA256", "X-Amz-Expires=300",
					"X-Amz-SignedHeaders=content-length%3Bcontent-type%3Bhost");
			// 브라우저 PUT이 Content-Type만 맞추면 되게, 체크섬 값이 서명에 끼어 있지 않다.
			assertThat(url.toLowerCase()).doesNotContain("x-amz-checksum", "x-amz-sdk-checksum-algorithm");
		}
	}

	@Test
	void onlyWebpAndExistingPlace() {
		Cookie admin = devLogin("admin@test.jieuni");
		long place = data.place("p", PlaceStatus.DRAFT, null);
		assertThat(issue(admin, body(place, "image/png", 1, 1)))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors.contentType").isEqualTo("WebP 사진만 올릴 수 있어요");
		assertThat(issue(admin, body(999999, "image/webp", 1, 1))).hasStatus(HttpStatus.NOT_FOUND);
		assertThat(issue(admin, body(null, "image/webp", 1, 1))).hasStatus(HttpStatus.BAD_REQUEST);
	}

	// 서명 URL은 크기를 밝힌 대로만 받으므로, 한도는 서명을 내주기 전에 본다.
	@Test
	void sizesAreRequiredAndLimited() {
		Cookie admin = devLogin("admin@test.jieuni");
		long place = data.place("p", PlaceStatus.DRAFT, null);
		long limit = 5L * 1024 * 1024;
		assertThat(issue(admin, body(place, "image/webp", limit, 1))).hasStatusOk();
		assertThat(issue(admin, body(place, "image/webp", limit + 1, 1)))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors['sizes.w1600']").isEqualTo("사진 파일이 너무 커요(5MB까지)");
		assertThat(issue(admin, body(place, "image/webp", 1, 0)))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors['sizes.w800']").isEqualTo("사진 크기가 필요해요");
		assertThat(issue(admin, "{\"placeId\":%d,\"contentType\":\"image/webp\"}".formatted(place)))
				.hasStatus(HttpStatus.BAD_REQUEST).bodyJson()
				.extractingPath("$.fieldErrors.sizes").isEqualTo("사진 크기가 필요해요");
	}

	@Test
	void userIs403() {
		long place = data.place("p", PlaceStatus.DRAFT, null);
		assertThat(issue(devLogin("a@test.jieuni"), body(place, "image/webp", 1, 1)))
				.hasStatus(HttpStatus.FORBIDDEN);
	}

}
