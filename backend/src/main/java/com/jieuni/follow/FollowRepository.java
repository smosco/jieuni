package com.jieuni.follow;

import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTileRow;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface FollowRepository extends JpaRepository<Follow, FollowId> {

	// 팔로우한 사람 여럿이 같은 공간에 크레딧이 있어도 exists라 한 번만 나온다.
	@Query("""
			select new com.jieuni.place.PlaceTileRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.publishedAt, ph)
			from Place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where p.status in :statuses and p.slug is not null
			  and exists (select 1 from PlaceCredit c, Follow f
			              where c.place = p and f.id.makerId = c.maker.id and f.id.userId = :userId)
			order by p.publishedAt desc, p.id desc""")
	List<PlaceTileRow> findFollowingPlaces(long userId, Collection<PlaceStatus> statuses, Limit limit);

	@Query("""
			select new com.jieuni.place.PlaceTileRow(p.id, p.slug, p.name, p.region, p.kind, p.status, p.publishedAt, ph)
			from Place p left join PlacePhoto ph on ph.place = p and ph.position = 0
			where p.status in :statuses and p.slug is not null
			  and exists (select 1 from PlaceCredit c, Follow f
			              where c.place = p and f.id.makerId = c.maker.id and f.id.userId = :userId)
			  and (p.publishedAt, p.id) < (:at, :id)
			order by p.publishedAt desc, p.id desc""")
	List<PlaceTileRow> findFollowingPlacesAfter(long userId, Collection<PlaceStatus> statuses, Instant at, long id,
			Limit limit);

	// 공간마다 팔로우한 사람들(크레딧 순서). 서비스가 첫 행을 캡션으로, 팔로우 시각들을 "새로운" 판정에 쓴다.
	@Query("""
			select new com.jieuni.follow.FollowedCreditRow(c.place.id, c.maker.name, f.createdAt)
			from PlaceCredit c, Follow f
			where c.place.id in :placeIds and f.id.makerId = c.maker.id and f.id.userId = :userId
			order by c.place.id, c.position""")
	List<FollowedCreditRow> findFollowedCredits(long userId, Collection<Long> placeIds);

	// 팔로우한 뒤에 공개된 공간만 센다. 늦게 팔로우한 사람의 지난 공간은 "새로운"이 아니다.
	@Query("""
			select exists (
			    select 1 from Place p
			    where p.status in :statuses and p.slug is not null and p.publishedAt > :since
			      and exists (select 1 from PlaceCredit c, Follow f
			                  where c.place = p and f.id.makerId = c.maker.id and f.id.userId = :userId
			                    and p.publishedAt > f.createdAt))""")
	boolean existsFollowingPlacePublishedAfter(long userId, Collection<PlaceStatus> statuses, Instant since);

	@Query("""
			select new com.jieuni.follow.FollowedMakerRow(m.id, m.slug, m.name, m.kind, m.region)
			from Follow f join Maker m on m.id = f.id.makerId
			where f.id.userId = :userId and m.slug is not null
			order by f.createdAt desc, m.id desc""")
	List<FollowedMakerRow> findFollowedMakers(long userId);

	@Query("select f.id.makerId from Follow f where f.id.userId = :userId and f.id.makerId in :makerIds")
	List<Long> findFollowedMakerIds(long userId, Collection<Long> makerIds);

	// 같은 요청이 동시에 와도 행은 하나다. 이미 있으면 처음 팔로우한 시각을 그대로 둔다.
	@Modifying
	@Query(nativeQuery = true, value = """
			insert into follows (user_id, maker_id, created_at) values (:userId, :makerId, :at)
			on conflict (user_id, maker_id) do nothing""")
	void insertIfAbsent(long userId, long makerId, Instant at);

	@Modifying
	@Query("delete from Follow f where f.id = :id")
	void deleteFollow(FollowId id);

}
