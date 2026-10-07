package com.jieuni.user;

import com.jieuni.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "users") // user는 PostgreSQL 예약어
public class User extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String provider;

	@Column(nullable = false)
	private String providerId;

	// 마지막으로 제공자가 검증한 이메일. 검증되지 않은 로그인에서는 이전 값을 유지한다.
	private String email;

	@Column(nullable = false)
	private String nickname;

	private String profileImageUrl;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private UserRole role;

	private Instant followingSeenAt;

	protected User() {
	}

	public User(String provider, String providerId, String email, String nickname, String profileImageUrl,
			UserRole role) {
		this.provider = provider;
		this.providerId = providerId;
		this.email = email;
		this.nickname = nickname;
		this.profileImageUrl = profileImageUrl;
		this.role = role;
	}

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	// 다시 로그인할 때 제공자 쪽에서 바뀌는 값. 닉네임은 사용자가 바꿨을 수 있어 건드리지 않는다.
	public void updateProfile(String email, String profileImageUrl) {
		if (email != null) {
			this.email = email;
		}
		this.profileImageUrl = profileImageUrl;
	}

	public void changeRole(UserRole role) {
		this.role = role;
	}

	public Long getId() {
		return id;
	}

	public String getProvider() {
		return provider;
	}

	public String getProviderId() {
		return providerId;
	}

	public String getEmail() {
		return email;
	}

	public String getNickname() {
		return nickname;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public UserRole getRole() {
		return role;
	}

	public Instant getFollowingSeenAt() {
		return followingSeenAt;
	}

}
