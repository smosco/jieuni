"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { MAKER_KIND_LABEL } from "@/shared/lib/labels";
import { useInfiniteScroll } from "@/shared/lib/useInfiniteScroll";
import form from "@/shared/ui/Form.module.css";
import { updatedAtLabel } from "../labels";
import { useAdminMakersPages } from "../queries";
import type { AdminMakerRow } from "../types";
import { AdminHead } from "./AdminHead";
import styles from "./Admin.module.css";

// 새 만든 사람은 공간 폼의 크레딧에서 만든다(목록에는 만들기 버튼이 없다).
export function AdminMakerList({ q }: { q: string }) {
  const router = useRouter();
  const [query, setQuery] = useState(q);
  const { data, isError, fetchNextPage, hasNextPage, isFetchingNextPage } = useAdminMakersPages(q);
  const sentinel = useInfiniteScroll(() => fetchNextPage(), hasNextPage && !isFetchingNextPage);
  const makers = data?.pages.flatMap((page) => page.items) ?? [];

  function search(e: FormEvent) {
    e.preventDefault();
    const next = query.trim();
    router.push(next ? `/admin/makers?q=${encodeURIComponent(next)}` : "/admin/makers");
  }

  return (
    <>
      <AdminHead title="만든 사람" />
      <form className={styles.search} onSubmit={search} role="search">
        <label htmlFor="admin-maker-q" className="visually-hidden">
          이름으로 찾기
        </label>
        <input
          id="admin-maker-q"
          type="search"
          value={query}
          placeholder="이름으로 찾기"
          onChange={(e) => setQuery(e.target.value)}
          className={`body-md ${form.input}`}
        />
      </form>

      {isError && <p className={`body-md ${styles.empty}`}>불러오지 못했어요. 다시 시도해 주세요</p>}
      {data && makers.length === 0 && <p className={`body-md ${styles.empty}`}>이 조건의 만든 사람이 없어요</p>}
      {makers.length > 0 && (
        <ul className={styles.rows}>
          {makers.map((maker) => (
            <li key={maker.id}>
              <MakerRow maker={maker} />
            </li>
          ))}
        </ul>
      )}
      <div ref={sentinel} />
    </>
  );
}

function MakerRow({ maker }: { maker: AdminMakerRow }) {
  const sub = [MAKER_KIND_LABEL[maker.kind], maker.region].filter(Boolean).join(" · ");
  return (
    <Link href={`/admin/makers/${maker.id}`} className={styles.row}>
      <div className={styles.rowLeft}>
        <p className={`body-md ${styles.rowName}`}>{maker.name}</p>
        <p className={`label-sm ${styles.sub}`}>{sub}</p>
      </div>
      <div className={styles.rowRight}>
        <p className={`label-sm ${styles.status}`}>{maker.placeCount > 0 ? `공간 ${maker.placeCount}곳` : "크레딧 없음"}</p>
        <p className={`label-sm ${styles.meta}`}>
          {updatedAtLabel(maker.updatedAt)}
          {!maker.slug && " · 프로필 없음"}
        </p>
      </div>
    </Link>
  );
}
