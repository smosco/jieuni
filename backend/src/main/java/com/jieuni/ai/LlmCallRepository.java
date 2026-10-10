package com.jieuni.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;

public interface LlmCallRepository extends JpaRepository<LlmCall, Long> {

	@Query("select coalesce(sum(c.costUsd), 0) from LlmCall c where c.createdAt >= :from and c.createdAt < :to")
	BigDecimal sumCost(Instant from, Instant to);

}
