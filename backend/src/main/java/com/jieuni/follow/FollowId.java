package com.jieuni.follow;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record FollowId(Long userId, Long makerId) implements Serializable {
}
