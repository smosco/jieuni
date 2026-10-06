package com.jieuni.maker;

import com.jieuni.common.BaseTimeEntity;
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
@Table(name = "makers")
public class Maker extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String slug;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MakerKind kind;

	private String region;

	private String intro;

	private String website;

	private String instagram;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "affiliation_id")
	private Maker affiliation;

	protected Maker() {
	}

	public Maker(String name, MakerKind kind) {
		this.name = name;
		this.kind = kind;
	}

	public Long getId() {
		return id;
	}

	public String getSlug() {
		return slug;
	}

	public String getName() {
		return name;
	}

	public MakerKind getKind() {
		return kind;
	}

	public String getRegion() {
		return region;
	}

	public String getIntro() {
		return intro;
	}

	public String getWebsite() {
		return website;
	}

	public String getInstagram() {
		return instagram;
	}

	public Maker getAffiliation() {
		return affiliation;
	}

}
