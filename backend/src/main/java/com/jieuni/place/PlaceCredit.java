package com.jieuni.place;

import com.jieuni.maker.Maker;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "place_credits")
public class PlaceCredit {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "maker_id", nullable = false)
	private Maker maker;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CreditRole role;

	@Column(nullable = false)
	private boolean verified;

	private String sourceUrl;

	@Column(nullable = false)
	private int position;

	protected PlaceCredit() {
	}

	PlaceCredit(Place place, Input input, int position) {
		this.place = place;
		this.maker = input.maker();
		this.role = input.role();
		this.verified = input.verified();
		this.sourceUrl = input.sourceUrl();
		this.position = position;
	}

	// 관리자 통째 교체의 한 줄. 순서(position)는 목록의 자리로 정한다.
	public record Input(Maker maker, CreditRole role, boolean verified, String sourceUrl) {
	}

	public Long getId() {
		return id;
	}

	public Place getPlace() {
		return place;
	}

	public Maker getMaker() {
		return maker;
	}

	public CreditRole getRole() {
		return role;
	}

	public boolean isVerified() {
		return verified;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public int getPosition() {
		return position;
	}

}
