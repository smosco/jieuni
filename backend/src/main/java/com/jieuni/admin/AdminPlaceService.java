package com.jieuni.admin;

import com.jieuni.common.ApiException;
import com.jieuni.common.Cursor;
import com.jieuni.common.CursorPage;
import com.jieuni.maker.Maker;
import com.jieuni.maker.MakerRepository;
import com.jieuni.place.CreditRole;
import com.jieuni.place.EmbedType;
import com.jieuni.place.PhotoUrls;
import com.jieuni.place.Place;
import com.jieuni.place.PlaceDetailResponse;
import com.jieuni.place.PlaceCredit;
import com.jieuni.place.PlaceEdit;
import com.jieuni.place.PlaceEmbed;
import com.jieuni.place.PlaceRepository;
import com.jieuni.place.PlaceService;
import com.jieuni.place.PlaceSource;
import com.jieuni.place.PlaceStatus;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AdminPlaceService {

	private final PlaceRepository places;
	private final MakerRepository makers;
	private final AdminPlaceRepository adminPlaces;
	private final PlaceService placeService;
	private final PhotoUrls photoUrls;
	private final PublishRules publishRules;
	private final Clock clock;

	// 공개 화면이 그릴 수 있는 임베드 주소. 그리지 못하는 주소로는 공개 조건만 채우게 된다.
	private static final Map<EmbedType, Set<String>> EMBED_HOSTS = Map.of(
			EmbedType.YOUTUBE, Set.of("youtube.com", "www.youtube.com", "m.youtube.com", "youtu.be"),
			EmbedType.INSTAGRAM, Set.of("instagram.com", "www.instagram.com"));
	private static final Pattern YOUTUBE_PATH = Pattern.compile("^/(?:shorts|embed)/[^/]+");
	private static final Pattern INSTAGRAM_POST = Pattern.compile("^/(?:p|reel)/[^/]+");

	// 전환 거절 문구에 쓰는 상태 이름. 관리자 화면의 상태 문구와 같다.
	private static final Map<PlaceStatus, String> STATUS_NAME = Map.of(
			PlaceStatus.SUGGESTED, "제보됨", PlaceStatus.DRAFT, "작성 중", PlaceStatus.PUBLISHED, "공개",
			PlaceStatus.PAUSED, "휴업", PlaceStatus.CLOSED, "폐업", PlaceStatus.REJECTED, "반려");

	AdminPlaceService(PlaceRepository places, MakerRepository makers, AdminPlaceRepository adminPlaces,
			PlaceService placeService, PhotoUrls photoUrls, PublishRules publishRules, Clock clock) {
		this.places = places;
		this.makers = makers;
		this.adminPlaces = adminPlaces;
		this.placeService = placeService;
		this.photoUrls = photoUrls;
		this.publishRules = publishRules;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public CursorPage<AdminPlaceRow> list(String statusParam, String q, String cursorParam, Integer sizeParam) {
		Set<PlaceStatus> statuses = parseStatus(statusParam);
		String pattern = q == null || q.isBlank() ? "%" : "%" + escapeLike(q.strip().toLowerCase(Locale.ROOT)) + "%";
		int size = Cursor.pageSize(sizeParam);
		Limit limit = Limit.of(size + 1);
		List<AdminPlaceRow> rows;
		if (cursorParam == null) {
			rows = adminPlaces.findRows(statuses, pattern, limit);
		} else {
			Cursor cursor = Cursor.decode(cursorParam);
			rows = adminPlaces.findRowsAfter(statuses, pattern, cursor.at(), cursor.id(), limit);
		}
		return CursorPage.of(rows, size, row -> new Cursor(row.updatedAt(), row.id()), row -> row);
	}

	@Transactional
	public AdminPlace create(String name, String region) {
		return toResponse(places.save(Place.draft(name, region)));
	}

	@Transactional(readOnly = true)
	public AdminPlace get(long id) {
		return toResponse(find(id));
	}

	@Transactional
	public AdminPlace edit(long id, PlaceEdit edit) {
		Place place = find(id);
		// 공개 URL은 한 번 정하면 바꾸지 않는다. 같은 값을 다시 보내는 것은 변경이 아니다.
		if (place.getPublishedAt() != null && !Objects.equals(place.getSlug(), edit.slug())) {
			throw new ApiException(HttpStatus.CONFLICT, "공개한 뒤에는 주소(slug)를 바꿀 수 없어요");
		}
		place.edit(edit);
		// 공개 페이지가 있는 공간은 필수 값을 비우면 공개 상세가 깨진다. 예외가 나면 트랜잭션이 롤백돼 수정이 남지 않는다.
		if (PlaceStatus.HAS_PAGE.contains(place.getStatus())) {
			requirePublishable(place, "공개된 공간이라 이 값은 비울 수 없어요");
		}
		// slug 중복은 DB 제약이 판단한다. 커밋까지 미루지 않고 여기서 내보내 응답 전에 409로 바꾼다.
		places.flush();
		return toResponse(place);
	}

	@Transactional
	public AdminPlace changeStatus(long id, PlaceStatus to) {
		Place place = find(id);
		if (!place.getStatus().canBecome(to)) {
			throw new ApiException(HttpStatus.CONFLICT,
					STATUS_NAME.get(place.getStatus()) + " 상태에서는 " + STATUS_NAME.get(to) + "(으)로 바꿀 수 없어요");
		}
		if (to == PlaceStatus.PUBLISHED) {
			requirePublishable(place, "공개할 수 없어요");
		}
		boolean firstPublish = to == PlaceStatus.PUBLISHED && place.getPublishedAt() == null;
		place.changeStatus(to, clock.instant());
		if (firstPublish) {
			lockMakerSlugs(place);
		}
		return toResponse(place);
	}

	public record CreditLine(long makerId, CreditRole role, boolean verified, String sourceUrl) {
	}

	@Transactional
	public AdminPlace replaceCredits(long id, List<CreditLine> lines) {
		Place place = find(id);
		Map<Long, Maker> found = new HashMap<>();
		makers.findAllById(lines.stream().map(CreditLine::makerId).toList()).forEach(m -> found.put(m.getId(), m));
		Map<String, String> errors = new LinkedHashMap<>();
		for (int i = 0; i < lines.size(); i++) {
			CreditLine line = lines.get(i);
			if (!found.containsKey(line.makerId())) {
				errors.put("credits[" + i + "].makerId", "없는 만든 사람이에요");
			}
			// DB CHECK(ck_place_credits_verified_source)가 있지만 500이 되기 전에 칸 오류로 돌려준다.
			if (line.verified() && line.sourceUrl() == null) {
				errors.put("credits[" + i + "].sourceUrl", "확인됨이면 출처 링크가 필요해요");
			}
		}
		requireNoErrors(errors);
		place.replaceCredits(lines.stream()
				.map(l -> new PlaceCredit.Input(found.get(l.makerId()), l.role(), l.verified(), l.sourceUrl()))
				.toList());
		if (PlaceStatus.HAS_PAGE.contains(place.getStatus())) {
			lockMakerSlugs(place);
		}
		return afterReplace(place);
	}

	@Transactional
	public AdminPlace replaceSources(long id, List<PlaceSource.Input> sources) {
		Place place = find(id);
		place.replaceSources(sources);
		return afterReplace(place);
	}

	@Transactional
	public AdminPlace replaceEmbeds(long id, List<PlaceEmbed.Input> embeds) {
		Place place = find(id);
		Map<String, String> errors = new LinkedHashMap<>();
		for (int i = 0; i < embeds.size(); i++) {
			PlaceEmbed.Input embed = embeds.get(i);
			if (!embeddable(embed)) {
				errors.put("embeds[" + i + "].url",
						embed.type() == EmbedType.YOUTUBE ? "유튜브 영상 주소를 넣어 주세요" : "인스타그램 게시물 주소를 넣어 주세요");
			}
		}
		requireNoErrors(errors);
		place.replaceEmbeds(embeds);
		return afterReplace(place);
	}

	// 목록을 바꿔도 공개 페이지가 있는 공간은 공개 조건을 지켜야 한다. 실패하면 롤백돼 교체가 남지 않는다.
	private AdminPlace afterReplace(Place place) {
		place.touch(clock.instant());
		if (PlaceStatus.HAS_PAGE.contains(place.getStatus())) {
			requirePublishable(place, "공개된 공간이라 이 값은 비울 수 없어요");
		}
		places.flush();
		return toResponse(place);
	}

	private static void requireNoErrors(Map<String, String> errors) {
		if (!errors.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "입력한 값을 확인해 주세요", errors);
		}
	}

	// 공개 페이지가 있는 공간의 크레딧에 든 만든 사람은 프로필 주소가 밖에 나간다.
	private void lockMakerSlugs(Place place) {
		Instant now = clock.instant();
		place.getCredits().forEach(credit -> credit.getMaker().lockSlug(now));
	}

	// 공개 화면(features/place/embed.ts)이 그릴 수 있는 모양만 받는다: 유튜브는 영상 id, 인스타그램은 게시물·릴스.
	private static boolean embeddable(PlaceEmbed.Input embed) {
		URI uri;
		try {
			uri = URI.create(embed.url());
		} catch (IllegalArgumentException e) {
			return false;
		}
		String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
		if (!EMBED_HOSTS.get(embed.type()).contains(host)) {
			return false;
		}
		String path = uri.getPath() == null ? "" : uri.getPath();
		if (embed.type() == EmbedType.INSTAGRAM) {
			return INSTAGRAM_POST.matcher(path).find();
		}
		if (host.equals("youtu.be")) {
			return path.length() > 1;
		}
		String v = queryParam(uri, "v");
		return v != null ? !v.isEmpty() : YOUTUBE_PATH.matcher(path).find();
	}

	private static String queryParam(URI uri, String name) {
		if (uri.getRawQuery() == null) {
			return null;
		}
		for (String pair : uri.getRawQuery().split("&")) {
			int eq = pair.indexOf('=');
			String key = eq < 0 ? pair : pair.substring(0, eq);
			if (key.equals(name)) {
				return eq < 0 ? "" : pair.substring(eq + 1);
			}
		}
		return null;
	}

	// 크레딧·출처·임베드 교체와 사진 삭제 뒤에도 같은 검사를 부른다.
	private void requirePublishable(Place place, String message) {
		Map<String, String> errors = publishRules.check(place);
		if (!errors.isEmpty()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, message, errors);
		}
	}

	private Place find(long id) {
		return places.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "공간을 찾을 수 없어요"));
	}

	private AdminPlace toResponse(Place place) {
		List<AdminPlace.Credit> credits = place.getCredits().stream()
				.map(c -> new AdminPlace.Credit(c.getPosition(), c.getRole(), c.isVerified(), c.getSourceUrl(),
						new PlaceDetailResponse.Maker(c.getMaker().getId(), c.getMaker().getSlug(),
								c.getMaker().getName(), c.getMaker().getKind()),
						c.getMaker().getId()))
				.toList();
		List<AdminPlace.AdminPhoto> photos = place.getPhotos().stream()
				.map(p -> new AdminPlace.AdminPhoto(p.getId(), p.getObjectKey(), p.getPosition(),
						photoUrls.of(p).url(), p.getWidth(), p.getHeight(), p.getAlt(), p.getCredit(), p.getLicense(),
						p.getSourceUrl()))
				.toList();
		AdminPlace.SuggestedBy suggestedBy = place.getSuggestedBy() == null ? null
				: new AdminPlace.SuggestedBy(place.getSuggestedBy().getId(), place.getSuggestedBy().getNickname());

		return new AdminPlace(place.getId(), place.getSlug(), place.getName(), place.getKind(), place.getRegion(),
				place.getAddress(), place.getLat(), place.getLng(), place.getAccess(), place.getVisitNote(),
				place.getSummary(), place.getImpression(),
				new PlaceDetailResponse.Senses(place.getSenseLight(), place.getSenseSound(), place.getSenseScent(),
						place.getSenseTouch()),
				place.getEssay(),
				place.getVisitedByEditorOn() == null ? null : YearMonth.from(place.getVisitedByEditorOn()).toString(),
				place.getStatus(), place.getPublishedAt(), credits,
				place.getSources().stream()
						.map(s -> new PlaceDetailResponse.Source(s.getTitle(), s.getUrl(), s.getType())).toList(),
				photos,
				place.getEmbeds().stream().map(e -> new PlaceDetailResponse.Embed(e.getType(), e.getUrl())).toList(),
				placeService.mapLinks(place), placeService.related(place), suggestedBy, place.getSuggestionReason(),
				place.getSuggestionLink(), place.getSuggestionMakerHint(), place.getCreatedAt(), place.getUpdatedAt(),
				place.getPublishedAt() != null);
	}

	private static Set<PlaceStatus> parseStatus(String value) {
		if (value == null || value.isBlank()) {
			return EnumSet.allOf(PlaceStatus.class);
		}
		return EnumSet.of(PlaceStatus.fromValue(value.strip())
				.orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "알 수 없는 상태예요: " + value.strip())));
	}

	// 검색어의 %·_를 글자 그대로 찾는다.
	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

}
