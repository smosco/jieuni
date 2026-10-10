package com.jieuni.feed;

import com.jieuni.common.PublicPagesChanged;
import com.jieuni.common.RevalidateClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// 피드 API는 조회 시점에 계산하지만 홈 페이지는 캐시라, 피드 응답이 바뀌는 시각에 비워야 바로 보인다.
@Component
class FeedRevalidateScheduler {

	// 정각에 비우면 직전 재생성이나 일찍 깬 스케줄러가 오늘 칸 없는 피드를 최대 1시간 굳힐 수 있어 5초 늦춘다.
	static final String OPENS = "5 0 8 * * MON-FRI";
	// 마지막 화면의 nextRelative("내일"→"오늘" 등)가 자정에 바뀐다.
	static final String MIDNIGHT = "5 0 0 * * *";

	private final RevalidateClient client;

	FeedRevalidateScheduler(RevalidateClient client) {
		this.client = client;
	}

	// 실패는 클라이언트가 로그로 남기고, 1시간 주기 재생성이 메운다.
	@Scheduled(cron = OPENS, zone = "Asia/Seoul")
	@Scheduled(cron = MIDNIGHT, zone = "Asia/Seoul")
	void scheduled() {
		client.revalidate(PublicPagesChanged.feed().tags());
	}

}
