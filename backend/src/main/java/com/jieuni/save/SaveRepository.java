package com.jieuni.save;

import com.jieuni.place.PlaceStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface SaveRepository extends JpaRepository<Save, SaveId> {

	@Query("""
			select new com.jieuni.save.SavedPlaceRow(p.id, p.slug, p.name, p.region, p.kind, p.status, s.updatedAt, ph)
			from Save s join Place p on p.id = s.id.placeId
			left join PlacePhoto ph on ph.place = p and ph.position = 0
			where s.id.userId = :userId and s.status = :status and p.status in :statuses and p.slug is not null
			order by s.updatedAt desc, p.id desc""")
	List<SavedPlaceRow> findSavedPlaces(long userId, SaveStatus status, Collection<PlaceStatus> statuses,
			Limit limit);

	@Query("""
			select new com.jieuni.save.SavedPlaceRow(p.id, p.slug, p.name, p.region, p.kind, p.status, s.updatedAt, ph)
			from Save s join Place p on p.id = s.id.placeId
			left join PlacePhoto ph on ph.place = p and ph.position = 0
			where s.id.userId = :userId and s.status = :status and p.status in :statuses and p.slug is not null
			  and (s.updatedAt, p.id) < (:at, :id)
			order by s.updatedAt desc, p.id desc""")
	List<SavedPlaceRow> findSavedPlacesAfter(long userId, SaveStatus status, Collection<PlaceStatus> statuses,
			Instant at, long id, Limit limit);

	@Query("select s from Save s where s.id.userId = :userId and s.id.placeId in :placeIds")
	List<Save> findByUserAndPlaces(long userId, Collection<Long> placeIds);

	// 같은 요청이 동시에 와도 행은 하나다. 같은 상태로 다시 저장하면 시각을 바꾸지 않는다(목록 순서 유지).
	@Modifying
	@Query(nativeQuery = true, value = """
			insert into saves (user_id, place_id, status, updated_at)
			values (:userId, :placeId, :status, :at)
			on conflict (user_id, place_id) do update
			set status = excluded.status,
			    updated_at = case when saves.status <> excluded.status then excluded.updated_at else saves.updated_at end""")
	void upsert(long userId, long placeId, String status, Instant at);

	@Modifying
	@Query("delete from Save s where s.id = :id")
	void deleteSave(SaveId id);

}
