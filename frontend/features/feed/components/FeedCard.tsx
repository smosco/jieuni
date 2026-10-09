"use client";

import Link from "next/link";
import { FollowButton } from "@/features/follow/components/FollowButton";
import { useFollowing } from "@/features/follow/queries";
import { SaveButtons } from "@/features/save/components/SaveButtons";
import { KIND_LABEL, ROLE_LABEL } from "@/shared/lib/labels";
import { track } from "@/shared/lib/track";
import { PhotoImage } from "@/shared/ui/PhotoImage";
import { WEEKDAY_EN, WEEKDAYS, weekPosition, weekdayKo } from "../copy";
import type { FeedPick, Weekday } from "../types";
import styles from "./Feed.module.css";

export function FeedCard({
  pick,
  openedWeekdays,
  priority,
  trackKey,
  onLeave,
}: {
  pick: FeedPick;
  openedWeekdays: Weekday[];
  priority: boolean;
  trackKey: string;
  onLeave: () => void;
}) {
  const { place, firstMaker } = pick;
  const closed = place.status === "closed";
  const following = useFollowing(firstMaker?.id ?? 0);
  // 피드에서는 팔로우한 사람에게만 숨긴다. 상태를 모르면(비로그인·받는 중·실패) 보인다.
  const showFollow = following !== true;

  return (
    <section className={styles.card} data-track={trackKey} aria-label={`${weekdayKo(pick.weekday)}의 공간: ${place.name}`}>
      {place.photo && (
        <PhotoImage photo={place.photo} sizes="(max-width: 480px) 100vw, 480px" className={styles.photo} priority={priority} />
      )}
      {/* 카드의 빈 곳을 누르면 상세로. 안의 링크·버튼은 이 위에 떠서 각자 동작한다. */}
      <Link
        href={`/places/${place.slug}`}
        className={styles.cardLink}
        aria-label={`${place.name} 자세히 보기`}
        onClick={() => {
          onLeave();
          track("card_open");
        }}
      />

      <div className={`label-sm ${styles.top}`}>
        <span>{WEEKDAY_EN[pick.weekday]}</span>
        <span className={styles.bars} aria-hidden="true">
          {WEEKDAYS.map((day) => (
            <span
              key={day}
              className={`${styles.bar} ${day === pick.weekday ? styles.barNow : openedWeekdays.includes(day) ? styles.barOpened : ""}`}
            />
          ))}
        </span>
        <span className={styles.position}>{weekPosition(pick.weekday)}</span>
      </div>

      <div className={styles.block}>
        <p className={`label-sm ${styles.dayLabel}`}>{weekdayKo(pick.weekday)}의 공간</p>
        <h2 className={`display-xl ${styles.text}`}>{place.name}</h2>
        <p className={`body-sm ${styles.meta}`}>
          {place.region} · {KIND_LABEL[place.kind]}
        </p>
        {closed && <p className={`body-sm ${styles.text}`}>이제는 만날 수 없는 공간이에요</p>}
        <p className={`body-lg ${styles.line}`}>{pick.line}</p>
        {firstMaker && (
          <div className={styles.makerRow}>
            <span className={`label-sm ${styles.role}`}>{ROLE_LABEL[firstMaker.role]}</span>
            {firstMaker.slug ? (
              <Link href={`/makers/${firstMaker.slug}`} className={`body-md ${styles.interactive}`} onClick={onLeave}>
                {firstMaker.name}
              </Link>
            ) : (
              <span className="body-md">{firstMaker.name}</span>
            )}
            {firstMaker.slug && showFollow && (
              <span className={styles.interactive}>
                <FollowButton makerId={firstMaker.id} makerName={firstMaker.name} onPhoto onLeave={onLeave} />
              </span>
            )}
          </div>
        )}
        {!closed && (
          <div className={styles.interactive}>
            <SaveButtons placeId={place.id} onPhoto onLeave={onLeave} />
          </div>
        )}
      </div>
    </section>
  );
}
