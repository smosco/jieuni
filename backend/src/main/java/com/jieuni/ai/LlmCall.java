package com.jieuni.ai;

import com.jieuni.place.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "llm_calls")
@EntityListeners(AuditingEntityListener.class)
public class LlmCall {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String purpose;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "place_id")
	private Place place;

	@Column(nullable = false)
	private String model;

	@Column(nullable = false)
	private int inputTokens;

	@Column(nullable = false)
	private int outputTokens;

	@Column(nullable = false)
	private BigDecimal costUsd;

	@Column(nullable = false)
	private int latencyMs;

	@Column(nullable = false)
	private int retries;

	@Column(nullable = false)
	private boolean success;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected LlmCall() {
	}

	public Long getId() {
		return id;
	}

	public String getPurpose() {
		return purpose;
	}

	public Place getPlace() {
		return place;
	}

	public String getModel() {
		return model;
	}

	public int getInputTokens() {
		return inputTokens;
	}

	public int getOutputTokens() {
		return outputTokens;
	}

	public BigDecimal getCostUsd() {
		return costUsd;
	}

	public int getLatencyMs() {
		return latencyMs;
	}

	public int getRetries() {
		return retries;
	}

	public boolean isSuccess() {
		return success;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
