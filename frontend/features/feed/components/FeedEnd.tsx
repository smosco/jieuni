"use client";

import Link from "next/link";
import { track } from "@/shared/lib/track";
import { endScreenCopy, weekPosition } from "../copy";
import type { FeedResponse } from "../types";
import styles from "./Feed.module.css";

const NEWSLETTER_URL = process.env.NEXT_PUBLIC_NEWSLETTER_URL;

export function FeedEnd({ feed, onLeave }: { feed: FeedResponse; onLeave: () => void }) {
  const { title, guide } = endScreenCopy(feed.nextRelative, feed.nextWeekday);
  const latest = feed.picks[0];

  return (
    <section className={styles.end} data-track="end" aria-label="이번 주 피드 끝">
      <div className={styles.endBlock}>
        {latest && (
          <p className={`label-sm ${styles.endPosition}`}>
            {latest.weekday} · {weekPosition(latest.weekday)}
          </p>
        )}
        <h2 className={`display-xl ${styles.endText}`}>{title}</h2>
        <p className={`body-lg ${styles.endGuide}`}>{guide}</p>
        <hr className={styles.rule} />
        <div className={styles.buttons}>
          <Link href="/places" className={`label-md ${styles.bigButton} ${styles.primary}`} onClick={onLeave}>
            전체 둘러보기 <span aria-hidden="true">→</span>
          </Link>
          {NEWSLETTER_URL && (
            <a
              href={NEWSLETTER_URL}
              target="_blank"
              rel="noopener noreferrer"
              className={`label-md ${styles.bigButton}`}
              onClick={() => track("newsletter_subscribe")}
            >
              뉴스레터 구독 <span aria-hidden="true">→</span>
            </a>
          )}
        </div>
      </div>
    </section>
  );
}
