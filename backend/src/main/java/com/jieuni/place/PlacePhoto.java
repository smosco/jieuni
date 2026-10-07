package com.jieuni.place;

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
@Table(name = "place_photos")
public class PlacePhoto {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Column(nullable = false)
	private String objectKey;

	@Column(nullable = false)
	private int width;

	@Column(nullable = false)
	private int height;

	private String alt;

	private String credit;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PhotoLicense license;

	private String sourceUrl;

	@Column(nullable = false)
	private int position;

	protected PlacePhoto() {
	}

	public Long getId() {
		return id;
	}

	public Place getPlace() {
		return place;
	}

	public String getObjectKey() {
		return objectKey;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public String getAlt() {
		return alt;
	}

	public String getCredit() {
		return credit;
	}

	public PhotoLicense getLicense() {
		return license;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public int getPosition() {
		return position;
	}

}
