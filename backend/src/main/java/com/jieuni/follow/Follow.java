package com.jieuni.follow;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

// 쓰기는 FollowRepository.insertIfAbsent(네이티브)로만 한다. 엔티티는 읽기용이다.
@Entity
@Table(name = "follows")
public class Follow {

	@EmbeddedId
	private FollowId id;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Follow() {
	}

	public FollowId getId() {
		return id;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
