"use client";

import Link from "next/link";
import type { CursorPage, PlaceKind, PlaceTile } from "@/shared/api/types";
import { useInfiniteScroll } from "@/shared/lib/useInfiniteScroll";
import { PlaceGrid } from "@/shared/ui/PlaceTile";
import { usePlacesPages } from "../queries";
import styles from "./Browse.module.css";

export function PlaceBrowseList({ kinds, initial }: { kinds: PlaceKind[]; initial: CursorPage<PlaceTile> }) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = usePlacesPages(kinds, initial);
  const sentinel = useInfiniteScroll(() => fetchNextPage(), hasNextPage && !isFetchingNextPage);
  const places = data.pages.flatMap((page) => page.items);

  if (places.length === 0) {
    return (
      <div className={styles.empty}>
        <p className={`body-md ${styles.emptyText}`}>아직 이 조건의 공간이 없어요</p>
        <div className={`label-md ${styles.emptyActions}`}>
          <Link href="/places">필터 초기화</Link>
          <Link href="/suggest">공간 소개하기 →</Link>
        </div>
      </div>
    );
  }

  return (
    <>
      <PlaceGrid places={places} variant="bleed" />
      <div ref={sentinel} />
    </>
  );
}
