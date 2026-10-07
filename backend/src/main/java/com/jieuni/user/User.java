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
