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

import java.time.Instant;

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

	private Instant slugLockedAt;

	protected Maker() {
	}

	public Maker(String name, MakerKind kind) {
		this.name = name;
		this.kind = kind;
	}

	public static Maker create(MakerEdit edit) {
		Maker maker = new Maker(edit.name(), edit.kind());
		maker.edit(edit);
		return maker;
	}

	// 관리자 폼의 저장 한 번. 필드 전부를 덮어쓴다(null은 비움). 공간 PATCH와 같은 모양.
	public void edit(MakerEdit edit) {
		this.slug = edit.slug();
		this.name = edit.name();
		this.kind = edit.kind();
		this.region = edit.region();
		this.intro = edit.intro();
		this.website = edit.website();
		this.instagram = edit.instagram();
		this.affiliation = edit.affiliation();
	}

	// 공개 페이지가 있는 공간의 크레딧에 들면 프로필 주소가 밖에 나간다. 그 뒤로는 크레딧에서 빠져도 잠긴 채다.
	// slug가 없으면 프로필도 없으니 잠그지 않는다(처음 정할 때 다시 부른다).
	public void lockSlug(Instant now) {
		if (slug != null && slugLockedAt == null) {
			slugLockedAt = now;
		}
	}

	public boolean isSlugLocked() {
		return slugLockedAt != null;
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
