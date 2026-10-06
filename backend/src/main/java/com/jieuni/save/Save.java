package com.jieuni.save;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.domain.Persistable;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "saves")
@EntityListeners(AuditingEntityListener.class)
public class Save implements Persistable<SaveId> {

	@EmbeddedId
	private SaveId id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SaveStatus status;

	@LastModifiedDate
	@Column(nullable = false)
	private Instant updatedAt;

	// id를 직접 넣으므로 save()가 SELECT로 존재 여부를 묻지 않게 새 객체임을 알린다.
	@Transient
	private boolean isNew = true;

	protected Save() {
	}

	public Save(Long userId, Long placeId, SaveStatus status) {
		this.id = new SaveId(userId, placeId);
		this.status = status;
	}

	@Override
	public SaveId getId() {
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

	public SaveStatus getStatus() {
		return status;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
