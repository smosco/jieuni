package com.jieuni.admin;

import com.jieuni.TestcontainersConfiguration;
import com.jieuni.place.Place;
import com.jieuni.place.PlaceStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 시드는 PublishRules를 거치지 않고 행을 직접 쓴다. 공개 페이지가 있는 시드 공간이 검증을 통과하지 못하면
// 관리자 폼에서 그 공간의 어떤 값도 저장할 수 없다. PublishRules가 패키지 전용이라 이 패키지에 둔다.
// DevProfileTest와 같은 설정이라 컨텍스트(적재된 시드)를 같이 쓴다.
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "FIXED_NOW=2026-10-07T22:59:00Z")
@ActiveProfiles("dev")
@Transactional
class DevSeedPublishableTest {

	@Autowired
	EntityManager em;

	@Autowired
	PublishRules rules;

	@Test
	void seededPlacesWithPagePassPublishRules() {
		List<Place> places = em.createQuery("select p from Place p where p.status in :statuses", Place.class)
				.setParameter("statuses", PlaceStatus.HAS_PAGE)
				.getResultList();

		assertThat(places).isNotEmpty();
		assertThat(places).allSatisfy(place -> assertThat(rules.check(place)).as(place.getSlug()).isEmpty());
	}

}
