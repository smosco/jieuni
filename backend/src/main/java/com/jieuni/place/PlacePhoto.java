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

	PlacePhoto(Place place, Input input, int position) {
		this.place = place;
		this.objectKey = input.objectKey();
		this.width = input.width();
		this.height = input.height();
		edit(input.alt(), input.credit(), input.license(), input.sourceUrl());
		this.position = position;
	}

	// 관리자가 등록하는 사진 한 장. 순서(position)는 맨 뒤로 정한다.
	public record Input(String objectKey, int width, int height, String alt, String credit, PhotoLicense license,
			String sourceUrl) {
	}

	public void edit(String alt, String credit, PhotoLicense license, String sourceUrl) {
		this.alt = alt;
		this.credit = credit;
		this.license = license;
		this.sourceUrl = sourceUrl;
	}

	void moveTo(int position) {
		this.position = position;
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
