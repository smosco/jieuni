package com.jieuni.follow;

import com.jieuni.common.ApiException;
import com.jieuni.common.Cursor;
import com.jieuni.common.CursorPage;
import com.jieuni.maker.MakerPhotoRow;
import com.jieuni.maker.MakerRepository;
import com.jieuni.maker.MakerSummary;
import com.jieuni.place.PhotoUrls;
import com.jieuni.place.PlacePhoto;
import com.jieuni.place.PlaceStatus;
import com.jieuni.place.PlaceTileRow;
import com.jieuni.user.User;
import com.jieuni.user.UserRepository;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FollowService {

	private final FollowRepository follows;
	private final MakerRepository makers;
	private final UserRepository users;
	private final PhotoUrls photoUrls;
	private final Clock clock;

	FollowService(FollowRepository follows, MakerRepository makers, UserRepository users, PhotoUrls photoUrls,
			Clock clock) {
		this.follows = follows;
		this.makers = makers;
		this.users = users;
		this.photoUrls = photoUrls;
		this.clock = clock;
	}

	@Transactional
	public void follow(long userId, long makerId) {
		if (!makers.existsByIdAndSlugIsNotNull(makerId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "만든 사람을 찾을 수 없어요");
		}
		follows.insertIfAbsent(userId, makerId, clock.instant());
	}

	@Transactional
	public void unfollow(long userId, long makerId) {
		follows.deleteFollow(new FollowId(userId, makerId));
	}

	@Transactional(readOnly = true)
	public CursorPage<FollowingPlace> places(long userId, String cursorParam, Integer sizeParam) {
		Instant seenAt = user(userId).getFollowingSeenAt();
		int size = Cursor.pageSize(sizeParam);
		Limit limit = Limit.of(size + 1);
		List<PlaceTileRow> rows;
		if (cursorParam == null) {
			rows = follows.findFollowingPlaces(userId, PlaceStatus.LISTED, limit);
		} else {
			Cursor cursor = Cursor.decode(cursorParam);
			rows = follows.findFollowingPlacesAfter(userId, PlaceStatus.LISTED, cursor.at(), cursor.id(), limit);
		}
		CursorPage<PlaceTileRow> page = CursorPage.of(rows, size, row -> new Cursor(row.publishedAt(), row.id()),
				row -> row);

		Map<Long, String> captions = new HashMap<>();
		Map<Long, List<Instant>> followedAts = new HashMap<>();
		if (!page.items().isEmpty()) {
			List<Long> ids = page.items().stream().map(PlaceTileRow::id).toList();
			for (FollowedCreditRow row : follows.findFollowedCredits(userId, ids)) {
				captions.putIfAbsent(row.placeId(), row.makerName());
				followedAts.computeIfAbsent(row.placeId(), id -> new ArrayList<>()).add(row.followedAt());
			}
		}
		List<FollowingPlace> items = page.items().stream()
				.map(row -> new FollowingPlace(row.id(), row.slug(), row.name(), row.region(), row.kind(),
						row.status(), photoUrls.of(row.photo()), captions.get(row.id()),
						isNew(row.publishedAt(), seenAt, followedAts.getOrDefault(row.id(), List.of()))))
				.toList();
		return new CursorPage<>(items, page.nextCursor());
	}

	@Transactional(readOnly = true)
	public List<MakerSummary> makers(long userId) {
		List<FollowedMakerRow> rows = follows.findFollowedMakers(userId);
		Map<Long, PlacePhoto> photos = new HashMap<>();
		if (!rows.isEmpty()) {
			List<Long> ids = rows.stream().map(FollowedMakerRow::id).toList();
			for (MakerPhotoRow row : makers.findLatestPhotos(ids, PlaceStatus.LISTED)) {
				photos.putIfAbsent(row.makerId(), row.photo());
			}
		}
		return rows.stream()
				.map(row -> new MakerSummary(row.id(), row.slug(), row.name(), row.kind(), row.region(),
						photoUrls.of(photos.get(row.id()))))
				.toList();
	}

	@Transactional
	public void markSeen(long userId) {
		user(userId).markFollowingSeen(clock.instant());
	}

	// /api/me의 탭 점. 팔로잉 목록의 isNew가 하나라도 있는가와 같은 식이다.
	@Transactional(readOnly = true)
	public boolean hasNew(long userId, Instant seenAt) {
		return follows.existsFollowingPlacePublishedAfter(userId, PlaceStatus.LISTED, orEpoch(seenAt));
	}

	// 새로운 = 탭을 마지막으로 본 뒤이면서, 그 공간의 팔로우한 사람 중 누군가를 팔로우한 뒤에 공개됐다.
	private static boolean isNew(Instant publishedAt, Instant seenAt, List<Instant> followedAts) {
		return publishedAt.isAfter(orEpoch(seenAt)) && followedAts.stream().anyMatch(publishedAt::isAfter);
	}

	// 팔로잉 탭을 한 번도 열지 않았으면 팔로우 시각만 본다.
	private static Instant orEpoch(Instant seenAt) {
		return seenAt == null ? Instant.EPOCH : seenAt;
	}

	private User user(long userId) {
		return users.findById(userId)
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "로그인이 필요해요"));
	}

}
