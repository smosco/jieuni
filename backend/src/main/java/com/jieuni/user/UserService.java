package com.jieuni.user;

import com.jieuni.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

	static final int NICKNAME_MAX = 20;

	private final UserRepository users;

	private final Set<String> adminEmails;

	UserService(UserRepository users, @Value("${jieuni.admin-emails:}") String adminEmails) {
		this.users = users;
		this.adminEmails = Arrays.stream(adminEmails.split(","))
				.map(email -> email.strip().toLowerCase(Locale.ROOT))
				.filter(email -> !email.isEmpty())
				.collect(Collectors.toUnmodifiableSet());
	}

	// 제공자 계정으로 사용자를 찾거나 만든다. email은 제공자가 검증한 값만 넘긴다(아니면 null).
	// 관리자 여부는 매 로그인마다 ADMIN_EMAILS로 다시 정한다.
	@Transactional
	public LoginUser login(String provider, String providerId, String email, String nickname, String profileImageUrl) {
		UserRole role = isAdminEmail(email) ? UserRole.ADMIN : UserRole.USER;
		User user = users.findByProviderAndProviderId(provider, providerId)
				.map(existing -> {
					existing.updateProfile(email, profileImageUrl);
					return existing;
				})
				.orElseGet(() -> users.save(new User(provider, providerId, email, truncate(nickname), profileImageUrl, role)));
		user.changeRole(role);
		return new LoginUser(user.getId());
	}

	@Transactional(readOnly = true)
	public MeResponse me(long userId) {
		return toResponse(find(userId));
	}

	@Transactional
	public MeResponse changeNickname(long userId, String nickname) {
		User user = find(userId);
		user.changeNickname(nickname.strip());
		return toResponse(user);
	}

	@Transactional(readOnly = true)
	public boolean isAdmin(long userId) {
		return users.findRoleById(userId).filter(UserRole.ADMIN::equals).isPresent();
	}

	// 기동 때 ADMIN_EMAILS와 users.role을 맞춘다. 목록에서 빠진 관리자는 다시 로그인하지 않아도 다음 요청부터 관리자가 아니다.
	@Transactional
	public void syncAdminRoles() {
		users.demoteAllAdmins();
		if (!adminEmails.isEmpty()) {
			users.promoteByEmails(adminEmails);
		}
	}

	private boolean isAdminEmail(String email) {
		return email != null && adminEmails.contains(email.toLowerCase(Locale.ROOT));
	}

	// 탈퇴 직후처럼 세션은 남았는데 사용자가 없으면 비로그인으로 본다.
	private User find(long userId) {
		return users.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"));
	}

	// users.nickname은 varchar(20)이고 PostgreSQL은 문자(코드 포인트)로 센다.
	private static String truncate(String nickname) {
		return nickname.codePoints().limit(NICKNAME_MAX)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}

	private static MeResponse toResponse(User user) {
		// hasNewFromFollowing은 팔로우 API와 함께 채운다.
		return new MeResponse(user.getId(), user.getNickname(), user.getProfileImageUrl(), user.getRole(), false);
	}

}
