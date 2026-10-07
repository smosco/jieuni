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
