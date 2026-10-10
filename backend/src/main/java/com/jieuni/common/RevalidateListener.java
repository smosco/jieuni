package com.jieuni.common;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 롤백된 변경으로 캐시를 비우면 다음 요청이 옛 값을 다시 굳힌다. 그래서 커밋된 뒤에만 부른다.
@Component
class RevalidateListener {

	private final RevalidateClient client;

	RevalidateListener(RevalidateClient client) {
		this.client = client;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	void on(PublicPagesChanged event) {
		client.revalidate(event.tags());
	}

}
