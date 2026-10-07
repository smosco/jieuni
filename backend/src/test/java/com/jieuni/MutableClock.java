package com.jieuni;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

// 테스트가 "지금"을 바꿔 가며 경계(07:59 / 08:00)를 확인하는 시계.
public class MutableClock extends Clock {

	private Instant now;

	public MutableClock(Instant now) {
		this.now = now;
	}

	public void set(Instant now) {
		this.now = now;
	}

	@Override
	public Instant instant() {
		return now;
	}

	@Override
	public ZoneId getZone() {
		return ZoneOffset.UTC;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return Clock.fixed(now, zone);
	}

}
