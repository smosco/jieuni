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
@Table(name = "place_embeds")
public class PlaceEmbed {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EmbedType type;

	@Column(nullable = false)
	private String url;

	@Column(nullable = false)
	private int position;

	protected PlaceEmbed() {
	}

	PlaceEmbed(Place place, Input input, int position) {
		this.place = place;
		this.type = input.type();
		this.url = input.url();
		this.position = position;
	}

	public record Input(EmbedType type, String url) {
	}

	public Long getId() {
		return id;
	}

	public Place getPlace() {
		return place;
	}

	public EmbedType getType() {
		return type;
	}

	public String getUrl() {
		return url;
	}

	public int getPosition() {
		return position;
	}

}
