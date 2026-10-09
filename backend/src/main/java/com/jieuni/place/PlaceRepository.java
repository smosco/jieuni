package com.jieuni.place;

import com.jieuni.common.SitemapEntry;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {

	// 크레딧 목록만 join fetch한다. 나머지 자식 목록은 배치 페치로 하나씩(둘 이상 join fetch하면 행이 곱으로 불어난다).
	@Query("""
			select p from Place p
			left join fetch p.credits c left join fetch c.maker
			where p.slug = :slug and p.status in :statuses""")
	Optional<Place> findWithCreditsBySlug(String slug, Collection<PlaceStatus> statuses);

	@Query("""
			select new com.jieuni.place.PlaceTileRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.publishedAt, ph)
			from Place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where p.status in :statuses and p.kind in :kinds and p.slug is not null
			order by p.publishedAt desc, p.id desc""")
	List<PlaceTileRow> findTiles(Collection<PlaceStatus> statuses, Collection<PlaceKind> kinds, Limit limit);

	@Query("""
			select new com.jieuni.place.PlaceTileRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.publishedAt, ph)
			from Place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where p.status in :statuses and p.kind in :kinds and p.slug is not null
			  and (p.publishedAt, p.id) < (:at, :id)
			order by p.publishedAt desc, p.id desc""")
	List<PlaceTileRow> findTilesAfter(Collection<PlaceStatus> statuses, Collection<PlaceKind> kinds, Instant at,
			long id, Limit limit);

	// 같은 만든 사람의 다른 공간. 크레딧이 여럿이어도 exists라 한 번만 나온다.
	@Query("""
			select new com.jieuni.place.PlaceTileRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.publishedAt, ph)
			from Place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where p.id <> :placeId and p.status in :statuses and p.slug is not null
			  and exists (select 1 from PlaceCredit c where c.place = p and c.maker.id in :makerIds)
			order by p.publishedAt desc, p.id desc""")
	List<PlaceTileRow> findRelated(long placeId, Collection<Long> makerIds, Collection<PlaceStatus> statuses,
			Limit limit);

	// 같은 이름은 앞뒤 공백·대소문자를 무시하고 본다.
	@Query("""
			select p.slug from Place p
			where lower(trim(p.name)) = lower(trim(:name)) and p.status in :statuses and p.slug is not null
			order by p.publishedAt desc""")
	List<String> findSlugsByName(String name, Collection<PlaceStatus> statuses, Limit limit);

	@Query("select count(p) from Place p where p.suggestedBy.id = :userId and p.createdAt >= :since")
	long countSuggestedSince(long userId, Instant since);

	@Query("select p.status from Place p where p.id = :id")
	Optional<PlaceStatus> findStatusById(long id);

	@Query("""
			select new com.jieuni.common.SitemapEntry(p.slug, p.updatedAt)
			from Place p where p.status in :statuses and p.slug is not null
			order by p.slug""")
	List<SitemapEntry> findSitemapEntries(Collection<PlaceStatus> statuses);

}
