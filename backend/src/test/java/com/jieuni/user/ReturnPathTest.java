package com.jieuni.user;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReturnPathTest {

	@ParameterizedTest
	@ValueSource(strings = { "/", "/places/quiet-hill", "/places/x?tab=photos&kind=garden", "/makers/%EC%9D%B4" })
	void keepsPathsInsideTheSite(String path) {
		assertThat(ReturnPath.sanitize(path)).isEqualTo(path);
	}

	// Location 헤더는 ASCII여야 하고, fragment가 남으면 뒤에 붙이는 ?login=error가 그 안으로 들어간다.
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"/makers/이로재 | /makers/%EC%9D%B4%EB%A1%9C%EC%9E%AC",
			"/places?region=서울 | /places?region=%EC%84%9C%EC%9A%B8",
			"/places/x#photos | /places/x",
			"/places?region=서울#map | /places?region=%EC%84%9C%EC%9A%B8",
			"/#top | /",
			"/makers/%EC%9D%B4로 | /makers/%EC%9D%B4%EB%A1%9C",
			"/a%zz | /a%25zz",
			"/a b\"c | /a%20b%22c",
			"/🙂 | /%F0%9F%99%82" })
	void encodesToAscii(String raw, String expected) {
		assertThat(ReturnPath.sanitize(raw)).isEqualTo(expected);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "https://evil.example", "//evil.example", "/\\evil.example", "javascript:alert(1)",
			"places/x", "/\t/evil.example", "/\n/evil.example", "/\r/evil.example", "/a\\b", "/places/x\u0000" })
	void fallsBackToRootForAnythingElse(String raw) {
		assertThat(ReturnPath.sanitize(raw)).isEqualTo("/");
	}

}
