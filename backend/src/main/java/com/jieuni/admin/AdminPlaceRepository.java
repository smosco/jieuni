package com.jieuni.admin;

import com.jieuni.place.Place;
import com.jieuni.place.PlaceStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

// 관리자 화면만 쓰는 공간 조회. place 패키지가 관리자 응답 모양을 몰라도 되게 따로 둔다.
interface AdminPlaceRepository extends Repository<Place, Long> {

	@Query("""
			select new com.jieuni.admin.AdminPlaceRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.updatedAt,
			       case when ph.id is null then false else true end)
			from Place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where p.status in :statuses and lower(p.name) like :pattern escape '\\'
			order by p.updatedAt desc, p.id desc""")
	List<AdminPlaceRow> findRows(Collection<PlaceStatus> statuses, String pattern, Limit limit);

	@Query("""
			select new com.jieuni.admin.AdminPlaceRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.updatedAt,
			       case when ph.id is null then false else true end)
			from Place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where p.status in :statuses and lower(p.name) like :pattern escape '\\'
			  and (p.updatedAt, p.id) < (:at, :id)
			order by p.updatedAt desc, p.id desc""")
	List<AdminPlaceRow> findRowsAfter(Collection<PlaceStatus> statuses, String pattern, Instant at, long id,
			Limit limit);

}
