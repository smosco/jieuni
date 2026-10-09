package com.jieuni.maker;

import com.jieuni.common.SitemapEntry;
import com.jieuni.place.PlaceStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MakerRepository extends JpaRepository<Maker, Long> {

	boolean existsByIdAndSlugIsNotNull(long id);

	@Query("select m from Maker m left join fetch m.affiliation where m.slug = :slug")
	Optional<Maker> findWithAffiliationBySlug(String slug);

	// 한 공간에 크레딧이 여럿이면 행도 여럿이다. position 순이라 서비스가 첫 행만 남긴다.
	@Query("""
			select new com.jieuni.maker.MakerPlaceRow(
			    p.id, p.slug, p.name, p.region, p.kind, p.status, p.publishedAt, ph, c.role)
			from PlaceCredit c join c.place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where c.maker.id = :makerId and p.status in :statuses and p.slug is not null
			order by p.publishedAt desc, p.id desc, c.position""")
	List<MakerPlaceRow> findProfilePlaces(long makerId, Collection<PlaceStatus> statuses);

	@Query("""
			select new com.jieuni.maker.MakerSummaryRow(m.id, m.slug, m.name, m.kind, m.region, max(p.publishedAt))
			from PlaceCredit c join c.maker m join c.place p
			where p.status in :statuses and m.slug is not null and p.slug is not null
			group by m.id, m.slug, m.name, m.kind, m.region
			order by max(p.publishedAt) desc, m.id desc""")
	List<MakerSummaryRow> findSummaries(Collection<PlaceStatus> statuses, Limit limit);

	@Query("""
			select new com.jieuni.maker.MakerSummaryRow(m.id, m.slug, m.name, m.kind, m.region, max(p.publishedAt))
			from PlaceCredit c join c.maker m join c.place p
			where p.status in :statuses and m.slug is not null and p.slug is not null
			group by m.id, m.slug, m.name, m.kind, m.region
			having max(p.publishedAt) < :at or (max(p.publishedAt) = :at and m.id < :id)
			order by max(p.publishedAt) desc, m.id desc""")
	List<MakerSummaryRow> findSummariesAfter(Collection<PlaceStatus> statuses, Instant at, long id, Limit limit);

	// 사진이 있는 공간만, 최근 공개순. 서비스가 만든 사람마다 첫 행을 쓴다.
	@Query("""
			select new com.jieuni.maker.MakerPhotoRow(c.maker.id, ph)
			from PlaceCredit c join c.place p join PlacePhoto ph on ph.place = p and ph.position = 0
			where c.maker.id in :makerIds and p.status in :statuses and p.slug is not null
			order by p.publishedAt desc, p.id desc""")
	List<MakerPhotoRow> findLatestPhotos(Collection<Long> makerIds, Collection<PlaceStatus> statuses);

	@Query("""
			select new com.jieuni.common.SitemapEntry(m.slug, m.updatedAt)
			from Maker m
			where m.slug is not null
			  and exists (select 1 from PlaceCredit c where c.maker = m and c.place.status in :statuses
			              and c.place.slug is not null)
			order by m.slug""")
	List<SitemapEntry> findSitemapEntries(Collection<PlaceStatus> statuses);

}
