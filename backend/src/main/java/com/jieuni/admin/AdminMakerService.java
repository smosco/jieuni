package com.jieuni.admin;

import com.jieuni.common.ApiException;
import com.jieuni.common.Cursor;
import com.jieuni.common.CursorPage;
import com.jieuni.common.PublicPagesChanged;
import com.jieuni.maker.Maker;
import com.jieuni.maker.MakerEdit;
import com.jieuni.maker.MakerKind;
import com.jieuni.maker.MakerProfileResponse;
import com.jieuni.maker.MakerRepository;
import com.jieuni.maker.MakerService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
class AdminMakerService {

	private final MakerRepository makers;
	private final AdminMakerRepository adminMakers;
	private final MakerService makerService;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	AdminMakerService(MakerRepository makers, AdminMakerRepository adminMakers, MakerService makerService,
			ApplicationEventPublisher events, Clock clock) {
		this.makers = makers;
		this.adminMakers = adminMakers;
		this.makerService = makerService;
		this.events = events;
		this.clock = clock;
	}

	record MakerForm(String slug, String name, MakerKind kind, String region, String intro, String website,
			String instagram, Long affiliationId) {
	}

	@Transactional(readOnly = true)
	public CursorPage<AdminMakerRow> list(String q, String cursorParam, Integer sizeParam) {
		String pattern = q == null || q.isBlank() ? "%" : "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
		int size = Cursor.pageSize(sizeParam);
		Limit limit = Limit.of(size + 1);
		List<AdminMakerRow> rows;
		if (cursorParam == null) {
			rows = adminMakers.findRows(pattern, limit);
		} else {
			Cursor cursor = Cursor.decode(cursorParam);
			rows = adminMakers.findRowsAfter(pattern, cursor.at(), cursor.id(), limit);
		}
		return CursorPage.of(rows, size, row -> new Cursor(row.updatedAt(), row.id()), row -> row);
	}

	@Transactional
	public AdminMaker create(MakerForm form) {
		Maker maker = makers.save(Maker.create(toEdit(form, null)));
		// slug 중복은 DB 제약이 판단한다. 커밋까지 미루지 않고 여기서 내보내 응답 전에 409로 바꾼다.
		makers.flush();
		return toResponse(maker);
	}

	@Transactional(readOnly = true)
	public AdminMaker get(long id) {
		return toResponse(find(id));
	}

	@Transactional
	public AdminMaker edit(long id, MakerForm form) {
		Maker maker = find(id);
		if (maker.isSlugLocked() && !Objects.equals(maker.getSlug(), form.slug())) {
			throw new ApiException(HttpStatus.CONFLICT, "공개된 공간에 연결된 뒤에는 주소(slug)를 바꿀 수 없어요");
		}
		String previousSlug = maker.getSlug();
		maker.edit(toEdit(form, id));
		// 이미 공개된 공간의 크레딧에 있는 사람에게 slug를 처음 정하면 그 순간 프로필이 밖에 나간다.
		boolean firstPublic = false;
		if (!maker.isSlugLocked() && maker.getSlug() != null && adminMakers.isCreditedOnPublishedPlace(id)) {
			maker.lockSlug(clock.instant());
			firstPublic = true;
		}
		makers.flush();
		AdminMaker response = toResponse(maker);
		// slug가 없어도 이름은 공개 공간의 크레딧에 보인다. 공개 페이지가 있는 공간이 없으면 밖에 보이는 곳이 없다.
		if (!response.places().isEmpty()) {
			events.publishEvent(PublicPagesChanged.maker(maker, previousSlug, response.places(), firstPublic));
		}
		return response;
	}

	private MakerEdit toEdit(MakerForm form, Long selfId) {
		Maker affiliation = null;
		if (form.affiliationId() != null) {
			if (form.affiliationId().equals(selfId)) {
				throw affiliationError("자기 자신은 소속이 될 수 없어요");
			}
			affiliation = makers.findById(form.affiliationId())
					.orElseThrow(() -> affiliationError("없는 만든 사람이에요"));
		}
		return new MakerEdit(form.slug(), form.name(), form.kind(), form.region(), form.intro(), form.website(),
				form.instagram(), affiliation);
	}

	private static ApiException affiliationError(String message) {
		return new ApiException(HttpStatus.BAD_REQUEST, "입력한 값을 확인해 주세요", Map.of("affiliationId", message));
	}

	private Maker find(long id) {
		return makers.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "만든 사람을 찾을 수 없어요"));
	}

	private AdminMaker toResponse(Maker maker) {
		Maker affiliation = maker.getAffiliation();
		return new AdminMaker(maker.getId(), maker.getSlug(), maker.getName(), maker.getKind(), maker.getRegion(),
				maker.getIntro(), maker.getWebsite(), maker.getInstagram(),
				affiliation == null ? null
						: new MakerProfileResponse.Affiliation(affiliation.getId(), affiliation.getSlug(),
								affiliation.getName()),
				makerService.places(maker.getId()), maker.isSlugLocked(),
				maker.getCreatedAt(), maker.getUpdatedAt());
	}

	// 검색어의 %·_를 글자 그대로 찾는다.
	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
