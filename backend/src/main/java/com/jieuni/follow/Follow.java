package com.jieuni.follow;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "follows")
@EntityListeners(AuditingEntityListener.class)
public class Follow implements Persistable<FollowId> {

	@EmbeddedId
	private FollowId id;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	// id를 직접 넣으므로 save()가 SELECT로 존재 여부를 묻지 않게 새 객체임을 알린다.
	@Transient
	private boolean isNew = true;

	protected Follow() {
	}

	public Follow(Long userId, Long makerId) {
		this.id = new FollowId(userId, makerId);
	}

	@Override
	public FollowId getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		isNew = false;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
