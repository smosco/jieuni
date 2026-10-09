package com.jieuni.place;

import com.jieuni.common.BaseTimeEntity;
import com.jieuni.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "places")
public class Place extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String slug;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	private PlaceKind kind;

	private String region;

	private String address;

	private BigDecimal lat;

	private BigDecimal lng;

	@Enumerated(EnumType.STRING)
	private Access access;

	private String visitNote;

	private String summary;

	private String impression;

	private String senseLight;

	private String senseSound;

	private String senseScent;

	private String senseTouch;

	private String essay;

	private LocalDate visitedByEditorOn;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PlaceStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "suggested_by")
	private User suggestedBy;

	private String suggestionReason;

	private String suggestionLink;

	private String suggestionMakerHint;

	private Instant publishedAt;

	// 자식 목록 넷은 Place가 통째로 소유한다(관리자 API의 "통째 교체"). 둘 이상을 join fetch하지 않는다(카테시안 곱·MultipleBagFetchException).
	@OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<PlaceCredit> credits = new ArrayList<>();

	@OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<PlaceSource> sources = new ArrayList<>();

	@OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<PlacePhoto> photos = new ArrayList<>();

	@OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<PlaceEmbed> embeds = new ArrayList<>();

	protected Place() {
	}

	// 사용자가 보낸 공간 제보. 이름·지역만 있고 나머지는 관리자가 채운다.
	public static Place suggested(String name, String region, User suggestedBy, String reason, String link,
			String makerHint) {
		Place place = new Place();
		place.name = name;
		place.region = region;
		place.status = PlaceStatus.SUGGESTED;
		place.suggestedBy = suggestedBy;
		place.suggestionReason = reason;
		place.suggestionLink = link;
		place.suggestionMakerHint = makerHint;
		return place;
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

	public PlaceKind getKind() {
		return kind;
	}

	public String getRegion() {
		return region;
	}

	public String getAddress() {
		return address;
	}

	public BigDecimal getLat() {
		return lat;
	}

	public BigDecimal getLng() {
		return lng;
	}

	public Access getAccess() {
		return access;
	}

	public String getVisitNote() {
		return visitNote;
	}

	public String getSummary() {
		return summary;
	}

	public String getImpression() {
		return impression;
	}

	public String getSenseLight() {
		return senseLight;
	}

	public String getSenseSound() {
		return senseSound;
	}

	public String getSenseScent() {
		return senseScent;
	}

	public String getSenseTouch() {
		return senseTouch;
	}

	public String getEssay() {
		return essay;
	}

	public LocalDate getVisitedByEditorOn() {
		return visitedByEditorOn;
	}

	public PlaceStatus getStatus() {
		return status;
	}

	public User getSuggestedBy() {
		return suggestedBy;
	}

	public String getSuggestionReason() {
		return suggestionReason;
	}

	public String getSuggestionLink() {
		return suggestionLink;
	}

	public String getSuggestionMakerHint() {
		return suggestionMakerHint;
	}

	public Instant getPublishedAt() {
		return publishedAt;
	}

	public List<PlaceCredit> getCredits() {
		return credits;
	}

	public List<PlaceSource> getSources() {
		return sources;
	}

	public List<PlacePhoto> getPhotos() {
		return photos;
	}

	public List<PlaceEmbed> getEmbeds() {
		return embeds;
	}

}
