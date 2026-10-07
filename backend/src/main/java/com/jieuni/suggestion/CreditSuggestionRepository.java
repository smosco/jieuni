package com.jieuni.suggestion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface CreditSuggestionRepository extends JpaRepository<CreditSuggestion, Long> {

	@Query("select count(c) from CreditSuggestion c where c.user.id = :userId and c.createdAt >= :since")
	long countSince(long userId, Instant since);

	@Query("""
			select new com.jieuni.suggestion.MySuggestions$Credit(c.id, p.name, c.role, c.makerName, c.status)
			from CreditSuggestion c join c.place p
			where c.user.id = :userId
			order by c.createdAt desc, c.id desc""")
	List<MySuggestions.Credit> findMine(long userId);

	@Query("""
			select new com.jieuni.suggestion.MySuggestions$Place(p.id, p.name, p.region, p.status, p.createdAt)
			from Place p
			where p.suggestedBy.id = :userId
			order by p.createdAt desc, p.id desc""")
	List<MySuggestions.Place> findMyPlaces(long userId);

}
