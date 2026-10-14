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
import java.util.Comparator;
import java.util.HashSet;
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

	// 관리자가 처음부터 쓰는 공간. slug는 공개 전에 채운다.
	public static Place draft(String name, String region) {
		Place place = new Place();
		place.name = name;
		place.region = region;
		place.status = PlaceStatus.DRAFT;
		return place;
	}

	public void edit(PlaceEdit edit) {
		this.slug = edit.slug();
		this.name = edit.name();
		this.kind = edit.kind();
		this.region = edit.region();
		this.address = edit.address();
		this.access = edit.access();
		this.visitNote = edit.visitNote();
		this.summary = edit.summary();
		this.impression = edit.impression();
		this.senseLight = edit.senseLight();
		this.senseSound = edit.senseSound();
		this.senseScent = edit.senseScent();
		this.senseTouch = edit.senseTouch();
		this.essay = edit.essay();
		this.visitedByEditorOn = edit.visitedByEditorOn();
	}

	// 관리자 API의 통째 교체: 옛 행은 orphanRemoval로 지워지고, 배열 순서가 position이 된다.
	// ActionQueue는 고아 삭제를 INSERT보다 먼저 두지만, id가 IDENTITY라 트랜잭션 안의 새 행은 persist 때 바로 INSERT된다.
	// 그래서 실제로는 새 행이 먼저 들어가고 옛 행이 나중에 지워진다(SQL 로그). 세 테이블에는 (place_id, position) 유니크가 없어 상관없다.
	public void replaceCredits(List<PlaceCredit.Input> inputs) {
		credits.clear();
		for (int i = 0; i < inputs.size(); i++) {
			credits.add(new PlaceCredit(this, inputs.get(i), i));
		}
	}

	public void replaceSources(List<PlaceSource.Input> inputs) {
		sources.clear();
		for (int i = 0; i < inputs.size(); i++) {
			sources.add(new PlaceSource(this, inputs.get(i), i));
		}
	}

	public void replaceEmbeds(List<PlaceEmbed.Input> inputs) {
		embeds.clear();
		for (int i = 0; i < inputs.size(); i++) {
			embeds.add(new PlaceEmbed(this, inputs.get(i), i));
		}
	}

	// 사진은 (place_id, position) 유니크(커밋 때 검사)가 있어 통째 교체 대신 한 장씩 다룬다. position 0이 대표 사진이다.
	public PlacePhoto addPhoto(PlacePhoto.Input input) {
		int last = photos.stream().mapToInt(PlacePhoto::getPosition).max().orElse(-1);
		PlacePhoto photo = new PlacePhoto(this, input, last + 1);
		photos.add(photo);
		return photo;
	}

	// 남은 사진을 0부터 다시 매겨 대표 사진(position 0)이 비지 않게 한다.
	public void removePhoto(PlacePhoto photo) {
		photos.remove(photo);
		renumberPhotos();
	}

	// photoIds가 지금 사진 id 전부와 정확히 같아야 한다. 아니면 아무것도 바꾸지 않고 false.
	public boolean reorderPhotos(List<Long> photoIds) {
		if (photoIds.size() != photos.size()
				|| !new HashSet<>(photoIds).equals(new HashSet<>(photos.stream().map(PlacePhoto::getId).toList()))) {
			return false;
		}
		photos.sort(Comparator.comparingInt(p -> photoIds.indexOf(p.getId())));
		renumberPhotos();
		return true;
	}

	private void renumberPhotos() {
		for (int i = 0; i < photos.size(); i++) {
			photos.get(i).moveTo(i);
		}
	}

	// 처음 공개한 시각은 한 번만 정한다. 휴업에서 다시 공개해도 둘러보기 순서가 바뀌지 않게.
	public void changeStatus(PlaceStatus to, Instant now) {
		this.status = to;
		if (to == PlaceStatus.PUBLISHED && publishedAt == null) {
			this.publishedAt = now;
		}
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
