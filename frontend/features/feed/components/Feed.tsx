"use client";

import { useEffect, useLayoutEffect, useRef } from "react";
import { useMeStateLoader } from "@/features/me/queries";
import { track } from "@/shared/lib/track";
import type { FeedResponse } from "../types";
import { FeedCard } from "./FeedCard";
import { FeedEnd } from "./FeedEnd";
import styles from "./Feed.module.css";

// 카드에서 상세·프로필로 나갈 때 몇 번째 카드였는지 남겨 두고, 돌아오면 그 카드로 맞춘다.
// 한 번 쓰면 지워서 탭 바로 "오늘"에 오면 맨 위부터 보인다.
const RETURN_KEY = "jieuni:feed-return";

export function Feed({ feed }: { feed: FeedResponse }) {
  const scroller = useRef<HTMLDivElement>(null);

  useLayoutEffect(() => {
    const saved = sessionStorage.getItem(RETURN_KEY);
    const el = scroller.current;
    if (saved === null || !el) return;
    sessionStorage.removeItem(RETURN_KEY);
    el.scrollTop = Number(saved) * el.clientHeight;
  }, []);

  // 화면의 대부분을 차지한 카드마다 한 번씩 보았다고 기록한다.
  useEffect(() => {
    const el = scroller.current;
    if (!el) return;
    const seen = new Set<string>();
    const observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          const key = (entry.target as HTMLElement).dataset.track;
          if (!entry.isIntersecting || !key || seen.has(key)) continue;
          seen.add(key);
          if (key === "end") track("feed_end");
          else track("feed_view", { position: Number(key) });
        }
      },
      { root: el, threshold: 0.6 },
    );
    el.querySelectorAll("[data-track]").forEach((child) => observer.observe(child));
    return () => observer.disconnect();
  }, []);

  const openedWeekdays = feed.picks.map((pick) => pick.weekday);
  // 같은 만든 사람이 여러 카드에 나올 수 있다.
  useMeStateLoader(
    feed.picks.map((pick) => pick.place.id),
    [...new Set(feed.picks.flatMap((pick) => (pick.firstMaker?.slug ? [pick.firstMaker.id] : [])))],
  );

  function rememberCard(index: number) {
    sessionStorage.setItem(RETURN_KEY, String(index));
  }

  return (
    <div ref={scroller} className={styles.scroller}>
      {feed.picks.map((pick, index) => (
        <FeedCard
          key={pick.date}
          pick={pick}
          openedWeekdays={openedWeekdays}
          priority={index === 0}
          trackKey={String(index + 1)}
          onLeave={() => rememberCard(index)}
        />
      ))}
      <FeedEnd feed={feed} onLeave={() => rememberCard(feed.picks.length)} />
    </div>
  );
}
