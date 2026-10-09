package com.jieuni.user;

import org.springframework.security.core.AuthenticatedPrincipal;

import java.io.Serializable;

// 세션에 저장되는 로그인 주체. 역할은 담지 않는다(세션이 30일이라 굳은 역할이 남지 않게, 필요할 때 DB에서 읽는다).
// getName()이 사용자 id라 Spring Session의 principal 색인으로 그 사용자의 세션을 찾을 수 있다.
public record LoginUser(long id) implements AuthenticatedPrincipal, Serializable {

	@Override
	public String getName() {
		return String.valueOf(id);
	}

}
