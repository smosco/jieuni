package com.jieuni.save;

import jakarta.persistence.Embeddable;

import java.io.Serializable;

@Embeddable
public record SaveId(Long userId, Long placeId) implements Serializable {
}
