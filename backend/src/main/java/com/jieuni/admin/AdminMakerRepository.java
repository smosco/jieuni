package com.jieuni.admin;

import com.jieuni.maker.Maker;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.List;

interface AdminMakerRepository extends Repository<Maker, Long> {

	@Query("""
			select new com.jieuni.admin.AdminMakerRow(m.id, m.slug, m.name, m.kind, m.region, count(distinct c.place.id),
			       m.updatedAt)
			from Maker m left join PlaceCredit c on c.maker = m
			where lower(m.name) like :pattern escape '\\'
			group by m.id, m.slug, m.name, m.kind, m.region, m.updatedAt
			order by m.updatedAt desc, m.id desc""")
	List<AdminMakerRow> findRows(String pattern, Limit limit);

	@Query("""
			select new com.jieuni.admin.AdminMakerRow(m.id, m.slug, m.name, m.kind, m.region, count(distinct c.place.id),
			       m.updatedAt)
			from Maker m left join PlaceCredit c on c.maker = m
			where lower(m.name) like :pattern escape '\\' and (m.updatedAt, m.id) < (:at, :id)
			group by m.id, m.slug, m.name, m.kind, m.region, m.updatedAt
			order by m.updatedAt desc, m.id desc""")
	List<AdminMakerRow> findRowsAfter(String pattern, Instant at, long id, Limit limit);

	// 한 번이라도 공개된 공간(published_at 있음)의 크레딧에 지금 있는가. slug를 처음 정할 때 잠글지 정한다.
	@Query("select count(c) > 0 from PlaceCredit c where c.maker.id = :makerId and c.place.publishedAt is not null")
	boolean isCreditedOnPublishedPlace(long makerId);

}
