package com.jieuni.feed;

import com.jieuni.common.BaseTimeEntity;
import com.jieuni.place.Place;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.LocalDate;

@Entity
@Table(name = "daily_picks")
public class DailyPick extends BaseTimeEntity implements Persistable<LocalDate> {

	@Id
	private LocalDate pickDate;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "place_id", nullable = false)
	private Place place;

	@Column(nullable = false)
	private String line;

	// id를 직접 넣으므로 save()가 SELECT로 존재 여부를 묻지 않게 새 객체임을 알린다.
	@Transient
	private boolean isNew = true;

	protected DailyPick() {
	}

	public DailyPick(LocalDate pickDate, Place place, String line) {
		this.pickDate = pickDate;
		this.place = place;
		this.line = line;
	}

	@Override
	public LocalDate getId() {
		return pickDate;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostPersist
	@PostLoad
	void markNotNew() {
		isNew = false;
	}

	// 같은 날 칸을 다른 공간·한 줄로 바꾼다. 새 객체를 같은 날짜로 save()하지 않는다(isNew라 INSERT가 PK 충돌).
	public void change(Place place, String line) {
		this.place = place;
		this.line = line;
	}

	public LocalDate getPickDate() {
		return pickDate;
	}

	public Place getPlace() {
		return place;
	}

	public String getLine() {
		return line;
	}

}
