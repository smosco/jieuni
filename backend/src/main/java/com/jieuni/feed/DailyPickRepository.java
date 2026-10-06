package com.jieuni.feed;

import com.jieuni.place.PlaceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DailyPickRepository extends JpaRepository<DailyPick, LocalDate> {

	@Query("""
			select new com.jieuni.feed.FeedPickRow(d.pickDate, d.line,
			    new com.jieuni.place.PlaceTileRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.publishedAt, ph))
			from DailyPick d join d.place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where d.pickDate between :from and :to and p.status in :statuses and p.slug is not null
			order by d.pickDate desc""")
	List<FeedPickRow> findOpened(LocalDate from, LocalDate to, Collection<PlaceStatus> statuses);

	// 공간마다 크레딧을 position 순으로. 서비스가 공간별 첫 행만 쓴다.
	@Query("""
			select new com.jieuni.feed.FirstMakerRow(c.place.id, m.id, m.slug, m.name, c.role)
			from PlaceCredit c join c.maker m
			where c.place.id in :placeIds
			order by c.place.id, c.position""")
	List<FirstMakerRow> findCreditsInOrder(Collection<Long> placeIds);

}
