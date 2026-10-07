"use client";

import Link from "next/link";
import type { CursorPage, MakerSummary } from "@/shared/api/types";
import { makerLabel } from "@/shared/lib/labels";
import { useInfiniteScroll } from "@/shared/lib/useInfiniteScroll";
import { PhotoImage } from "@/shared/ui/PhotoImage";
import { useMakersPages } from "../queries";
import styles from "./Browse.module.css";

export function MakerBrowseList({ initial }: { initial: CursorPage<MakerSummary> }) {
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } = useMakersPages(initial);
  const sentinel = useInfiniteScroll(() => fetchNextPage(), hasNextPage && !isFetchingNextPage);
  const makers = data.pages.flatMap((page) => page.items);

  return (
    <>
      <ul className={styles.makers}>
        {makers.map((maker) => (
          <li key={maker.id}>
            <Link href={`/makers/${maker.slug}`} className={styles.makerRow}>
              <span className={styles.thumb}>
                {maker.photo && <PhotoImage photo={maker.photo} sizes="48px" className={styles.thumbPhoto} />}
              </span>
              <span className={styles.makerText}>
                <span className="body-md">{maker.name}</span>
                <span className={`label-sm ${styles.makerLabel}`}>{makerLabel(maker.kind, maker.region)}</span>
              </span>
            </Link>
          </li>
        ))}
      </ul>
      <div ref={sentinel} />
    </>
  );
}
