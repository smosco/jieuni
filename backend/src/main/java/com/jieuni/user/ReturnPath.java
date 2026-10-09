package com.jieuni.user;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.nio.charset.StandardCharsets;

// 로그인 뒤 돌아갈 경로. 우리 사이트 안의 경로만 허용해 오픈 리디렉트를 막는다.
final class ReturnPath {

	private static final String ATTRIBUTE = "jieuni.returnPath";

	private ReturnPath() {
	}

	// 경로 · 쿼리에 그대로 둘 수 있는 ASCII 문자. 나머지(한글, 공백, 따옴표 등)는 퍼센트 인코딩한다.
	private static final String ALLOWED = "-._~!$&'()*+,;=:@/?";

	// "/"로 시작하는 경로만. "//host"는 다른 사이트로 가는 주소이고, 브라우저는 역슬래시를 슬래시로 바꾸고
	// 탭·줄바꿈을 지우므로("/\t/evil" → "//evil") 제어 문자나 역슬래시가 하나라도 있으면 버린다.
	// fragment(# 뒤)는 잘라낸다. 남으면 뒤에 붙이는 ?login=error가 fragment 안으로 들어간다.
	// 남은 값은 ASCII로 인코딩해 돌려준다. Tomcat은 ASCII 밖의 Location 헤더를 내보내지 않는다(빈 화면).
	static String sanitize(String raw) {
		if (raw == null || !raw.startsWith("/") || raw.startsWith("//")) {
			return "/";
		}
		for (int i = 0; i < raw.length(); i++) {
			char c = raw.charAt(i);
			if (c < 0x20 || c == 0x7f || c == '\\') {
				return "/";
			}
		}
		int fragment = raw.indexOf('#');
		return encode(fragment < 0 ? raw : raw.substring(0, fragment));
	}

	// 이미 %XX인 부분은 그대로 둔다(두 번 인코딩하지 않는다).
	private static String encode(String path) {
		StringBuilder out = new StringBuilder();
		for (int i = 0; i < path.length(); ) {
			int c = path.codePointAt(i);
			if (c == '%' && i + 2 < path.length() && isHex(path.charAt(i + 1)) && isHex(path.charAt(i + 2))) {
				out.append(path, i, i + 3);
				i += 3;
				continue;
			}
			if (c < 0x80 && (Character.isLetterOrDigit(c) || ALLOWED.indexOf(c) >= 0)) {
				out.append((char) c);
			}
			else {
				for (byte b : new String(Character.toChars(c)).getBytes(StandardCharsets.UTF_8)) {
					out.append('%').append(String.format("%02X", b & 0xff));
				}
			}
			i += Character.charCount(c);
		}
		return out.toString();
	}

	private static boolean isHex(char c) {
		return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
	}

	static void save(HttpServletRequest request, String raw) {
		request.getSession().setAttribute(ATTRIBUTE, sanitize(raw));
	}

	static String take(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null || !(session.getAttribute(ATTRIBUTE) instanceof String path)) {
			return "/";
		}
		session.removeAttribute(ATTRIBUTE);
		return path;
	}

}
