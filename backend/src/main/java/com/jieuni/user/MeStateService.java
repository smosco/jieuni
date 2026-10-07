package com.jieuni.user;

import com.jieuni.common.ApiException;
import com.jieuni.follow.FollowRepository;
import com.jieuni.save.Save;
import com.jieuni.save.SaveRepository;
import com.jieuni.save.SaveStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 공개 화면(캐시된 HTML) 위에 얹는 사용자별 값. 화면에 보이는 공간·만든 사람 id만 묻는다.
@Service
class MeStateService {

	private static final int MAX_IDS = 100;

	private final SaveRepository saves;
	private final FollowRepository follows;

	MeStateService(SaveRepository saves, FollowRepository follows) {
		this.saves = saves;
		this.follows = follows;
	}

	record MeState(Map<Long, SaveStatus> saves, List<Long> follows) {
	}

	@Transactional(readOnly = true)
	MeState state(long userId, String placesParam, String makersParam) {
		List<Long> placeIds = parseIds("places", placesParam);
		List<Long> makerIds = parseIds("makers", makersParam);
		Map<Long, SaveStatus> saveMap = new LinkedHashMap<>();
		if (!placeIds.isEmpty()) {
			for (Save save : saves.findByUserAndPlaces(userId, placeIds)) {
				saveMap.put(save.getId().placeId(), save.getStatus());
			}
		}
		List<Long> followed = makerIds.isEmpty() ? List.of() : follows.findFollowedMakerIds(userId, makerIds);
		return new MeState(saveMap, followed);
	}

	private static List<Long> parseIds(String name, String param) {
		if (param == null || param.isBlank()) {
			return List.of();
		}
		try {
			List<Long> ids = Arrays.stream(param.split(",")).map(String::strip).map(Long::parseLong).distinct().toList();
			if (ids.size() > MAX_IDS) {
				throw new ApiException(HttpStatus.BAD_REQUEST, name + "는 한 번에 " + MAX_IDS + "개까지 물을 수 있어요");
			}
			return ids;
		} catch (NumberFormatException e) {
			throw new ApiException(HttpStatus.BAD_REQUEST, name + "는 쉼표로 나눈 숫자여야 해요");
		}
	}

}
