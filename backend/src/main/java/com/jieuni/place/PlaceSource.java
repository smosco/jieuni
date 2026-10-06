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
@Table(name = "place_sources")
public class PlaceSource {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String url;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SourceType type;

	@Column(nullable = false)
	private int position;

	protected PlaceSource() {
	}

	public Long getId() {
		return id;
	}

	public Place getPlace() {
		return place;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public SourceType getType() {
		return type;
	}

	public int getPosition() {
		return position;
	}

}
