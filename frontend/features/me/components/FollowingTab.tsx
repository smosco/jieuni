"use client";

import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useEffect, useRef } from "react";
import { FollowButton } from "@/features/follow/components/FollowButton";
import { MAKER_KIND_LABEL } from "@/shared/lib/labels";
import { useInfiniteScroll } from "@/shared/lib/useInfiniteScroll";
import { PlaceGrid } from "@/shared/ui/PlaceTile";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { markFollowingSeen } from "../api";
import { useFollowingMakers, useFollowingPlacesPages } from "../queries";
import type { Me } from "../types";
import { Empty, LoadError } from "./SavedTab";
import styles from "./MySpace.module.css";

export function FollowingTab() {
  const queryClient = useQueryClient();
  const places = useFollowingPlacesPages();
  const makers = useFollowingMakers();
  const sentinel = useInfiniteScroll(() => places.fetchNextPage(), places.hasNextPage && !places.isFetchingNextPage);
  const markedSeen = useRef(false);

  // 탭을 열면 본 것으로 남긴다. "새로운"이 보이도록 목록을 받은 뒤에 보낸다.
  // 목록은 이 화면에 있는 동안 다시 받지 않으므로(useFollowingPlacesPages) 떠날 때까지 "새로운"이 남는다.
  useEffect(() => {
    if (places.status !== "success" || markedSeen.current) return;
    markedSeen.current = true;
    markFollowingSeen()
      .then(() => queryClient.setQueryData<Me | null>(["me"], (me) => me && { ...me, hasNewFromFollowing: false }))
      // 실패하면 점이 남고, 다음에 탭을 열 때 다시 보낸다.
      .catch(() => {});
  }, [places.status, queryClient]);

  if (places.isError || makers.isError) return <LoadError />;
  if (!places.data || !makers.data) return null;
  const items = places.data.pages.flatMap((page) => page.items);
  if (items.length === 0 && makers.data.items.length === 0) {
    return <Empty text="아직 팔로우한 사람이 없어요" href="/makers" link="만든 사람 둘러보기" />;
  }

  return (
    <>
      {items.length > 0 && (
        <section>
          <div className={styles.sectionHead}>
            <SectionLabel>팔로우한 사람들의 공간</SectionLabel>
          </div>
          <PlaceGrid places={items} variant="bleed" />
          <div ref={sentinel} />
        </section>
      )}
      <section className={styles.makersSection}>
        <SectionLabel>팔로우한 만든 사람</SectionLabel>
        <ul className={styles.rows}>
          {makers.data.items.map((maker) => (
            <li key={maker.id} className={styles.row}>
              <span className={styles.rowText}>
                <Link href={`/makers/${maker.slug}`} className="body-md">
                  {maker.name}
                </Link>
                <span className={`label-sm ${styles.sub}`}>{MAKER_KIND_LABEL[maker.kind]}</span>
              </span>
              <FollowButton makerId={maker.id} makerName={maker.name} />
            </li>
          ))}
        </ul>
      </section>
    </>
  );
}
