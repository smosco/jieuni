package com.jieuni.user;

public record MeResponse(long id, String nickname, String profileImageUrl, UserRole role,
		boolean hasNewFromFollowing) {
}
