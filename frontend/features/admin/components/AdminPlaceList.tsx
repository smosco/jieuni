"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { KIND_LABEL } from "@/shared/lib/labels";
import { useInfiniteScroll } from "@/shared/lib/useInfiniteScroll";
import form from "@/shared/ui/Form.module.css";
import { TopTabs } from "@/shared/ui/TopTabs";
import { STATUS_LABEL, STATUS_ORDER, placesHref, updatedAtLabel } from "../labels";
import { useAdminPlacesPages } from "../queries";
import type { AdminPlaceStatus, AdminPlaceSummary } from "../types";
import { AdminHead } from "./AdminHead";
import styles from "./Admin.module.css";

export function AdminPlaceList({ status, q }: { status: AdminPlaceStatus | null; q: string }) {
  const router = useRouter();
  const [query, setQuery] = useState(q);
  const { data, isError, fetchNextPage, hasNextPage, isFetchingNextPage } = useAdminPlacesPages(status, q);
  const sentinel = useInfiniteScroll(() => fetchNextPage(), hasNextPage && !isFetchingNextPage);
  const places = data?.pages.flatMap((page) => page.items) ?? [];

  function search(e: FormEvent) {
    e.preventDefault();
    router.push(placesHref(status, query.trim()));
  }

  return (
    <>
      <AdminHead title="공간" newPlace />
      <TopTabs
        className={styles.tabs}
        items={[
          { href: placesHref(null, q), label: "전체", active: status === null },
          ...STATUS_ORDER.map((s) => ({ href: placesHref(s, q), label: STATUS_LABEL[s], active: status === s })),
        ]}
      />
      <form className={styles.search} onSubmit={search} role="search">
        <label htmlFor="admin-place-q" className="visually-hidden">
          이름으로 찾기
        </label>
        <input
          id="admin-place-q"
          type="search"
          value={query}
          placeholder="이름으로 찾기"
          onChange={(e) => setQuery(e.target.value)}
          className={`body-md ${form.input}`}
        />
      </form>

      {isError && <p className={`body-md ${styles.empty}`}>불러오지 못했어요. 다시 시도해 주세요</p>}
      {data && places.length === 0 && <p className={`body-md ${styles.empty}`}>이 조건의 공간이 없어요</p>}
      {places.length > 0 && (
        <ul className={styles.rows}>
          {places.map((place) => (
            <li key={place.id}>
              <PlaceRow place={place} />
            </li>
          ))}
        </ul>
      )}
      <div ref={sentinel} />
    </>
  );
}

function PlaceRow({ place }: { place: AdminPlaceSummary }) {
  const sub = [place.region, place.kind && KIND_LABEL[place.kind]].filter(Boolean).join(" · ");
  return (
    <Link href={`/admin/places/${place.id}`} className={styles.row}>
      <div className={styles.rowLeft}>
        <p className={`body-md ${styles.rowName}`}>{place.name}</p>
        {sub && <p className={`label-sm ${styles.sub}`}>{sub}</p>}
      </div>
      <div className={styles.rowRight}>
        <p className={`label-sm ${styles.status}`}>{STATUS_LABEL[place.status]}</p>
        <p className={`label-sm ${styles.meta}`}>
          {updatedAtLabel(place.updatedAt)}
          {!place.hasPhoto && " · 사진 없음"}
        </p>
      </div>
    </Link>
  );
}
