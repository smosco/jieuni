"use client";

import Link from "next/link";
import type { SaveStatus } from "@/features/save/types";
import { useInfiniteScroll } from "@/shared/lib/useInfiniteScroll";
import { PlaceGrid } from "@/shared/ui/PlaceTile";
import { useSavesPages } from "../queries";
import styles from "./MySpace.module.css";

export function SavedTab({ status }: { status: SaveStatus }) {
  const { data, isError, fetchNextPage, hasNextPage, isFetchingNextPage } = useSavesPages(status);
  const sentinel = useInfiniteScroll(() => fetchNextPage(), hasNextPage && !isFetchingNextPage);

  if (isError) return <LoadError />;
  if (!data) return null;
  const places = data.pages.flatMap((page) => page.items);
  if (places.length === 0) {
    return <Empty text="아직 저장한 공간이 없어요" href="/" link="오늘의 공간 보기" />;
  }
  return (
    <div className={styles.grid}>
      <PlaceGrid places={places} variant="bleed" />
      <div ref={sentinel} />
    </div>
  );
}

export function Empty({ text, href, link }: { text: string; href: string; link: string }) {
  return (
    <div className={styles.empty}>
      <p className={`body-md ${styles.emptyText}`}>{text}</p>
      <Link href={href} className={`label-md ${styles.emptyLink}`}>
        {link} →
      </Link>
    </div>
  );
}

export function LoadError() {
  return (
    <div className={styles.empty}>
      <p className={`body-md ${styles.emptyText}`}>불러오지 못했어요. 다시 시도해 주세요</p>
    </div>
  );
}
