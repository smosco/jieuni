package com.jieuni.suggestion;

import com.jieuni.place.CreditRole;
import com.jieuni.place.Place;
import com.jieuni.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "credit_suggestions")
@EntityListeners(AuditingEntityListener.class)
public class CreditSuggestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private User user; // 탈퇴하면 null(ON DELETE SET NULL)

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CreditRole role;

	@Column(nullable = false)
	private String makerName;

	private String sourceUrl;

	@Column(nullable = false)
	private boolean isSelf;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SuggestionStatus status;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	private Instant reviewedAt;

	protected CreditSuggestion() {
	}

	public CreditSuggestion(User user, Place place, CreditRole role, String makerName, String sourceUrl,
			boolean isSelf) {
		this.user = user;
		this.place = place;
		this.role = role;
		this.makerName = makerName;
		this.sourceUrl = sourceUrl;
		this.isSelf = isSelf;
		this.status = SuggestionStatus.PENDING;
	}

	// 관리자가 검토한다. 대기 중인 제보만 바꿀 수 있는지는 서비스가 본다.
	public void review(SuggestionStatus to, Instant at) {
		this.status = to;
		this.reviewedAt = at;
	}

	public Long getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public Place getPlace() {
		return place;
	}

	public CreditRole getRole() {
		return role;
	}

	public String getMakerName() {
		return makerName;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public boolean isSelf() {
		return isSelf;
	}

	public SuggestionStatus getStatus() {
		return status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

}
