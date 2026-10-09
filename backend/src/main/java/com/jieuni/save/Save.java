package com.jieuni.save;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;

// 쓰기는 SaveRepository.upsert(네이티브)로만 한다. 엔티티는 읽기용이다.
@Entity
@Table(name = "saves")
public class Save {

	@EmbeddedId
	private SaveId id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SaveStatus status;

	@Column(nullable = false)
	private Instant updatedAt;

	protected Save() {
	}

	public SaveId getId() {
		return id;
	}

	public SaveStatus getStatus() {
		return status;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
