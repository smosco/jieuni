package com.jieuni.follow;

import java.time.Instant;

// 공간에 크레딧이 있는, 내가 팔로우한 사람 한 명과 그 사람을 팔로우한 시각.
public record FollowedCreditRow(Long placeId, String makerName, Instant followedAt) {
}
