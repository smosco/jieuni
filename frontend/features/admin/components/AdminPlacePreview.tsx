"use client";

import Link from "next/link";
import { PlaceDetailView } from "@/features/place/components/PlaceDetailView";
import type { PlaceDetail } from "@/features/place/types";
import { ApiError } from "@/shared/api/client";
import type { PlaceKind } from "@/shared/api/types";
import { STATUS_LABEL } from "../labels";
import { useAdminPlace } from "../queries";
import type { AdminPlace } from "../types";
import { AdminHead } from "./AdminHead";
import admin from "./Admin.module.css";
import styles from "./PlaceForm.module.css";

// 공개 상세는 공개된 공간의 모양을 받는다. 작성 중인 공간의 빈 값은 그대로 넘기고(컴포넌트가 없는 값은 숨긴다),
// 휴업·폐업이 아니면 띠 없이 그린다.
function toPreview(place: AdminPlace): PlaceDetail {
  return {
    ...place,
    slug: place.slug ?? "",
    kind: place.kind as PlaceKind,
    region: place.region ?? "",
    status: place.status === "paused" || place.status === "closed" ? place.status : "published",
    publishedAt: place.publishedAt ?? place.updatedAt,
  };
}

export function AdminPlacePreview({ id }: { id: number }) {
  const { data: place, error, isPending } = useAdminPlace(id);

  if (isPending) return null;
  if (!place) {
    const missing = error instanceof ApiError && error.status === 404;
    return (
      <>
        <AdminHead title="미리보기" />
        <p className={`body-md ${admin.empty}`}>
          {missing ? "찾을 수 없는 공간이에요" : "불러오지 못했어요. 다시 시도해 주세요"}
        </p>
      </>
    );
  }
  return (
    <>
      <AdminHead title="미리보기" status={STATUS_LABEL[place.status]} />
      <div className={styles.previewHead}>
        <p className={`body-sm ${styles.none}`}>저장한 내용이 공개 화면에서 이렇게 보여요.</p>
        <Link href={`/admin/places/${place.id}`} className={`label-md ${admin.shortcut}`}>
          폼으로 →
        </Link>
      </div>
      {/* 보기만 한다. 저장·팔로우·크레딧 알려주기가 실제로 눌리지 않게 막는다. */}
      <div className={styles.preview} inert>
        <PlaceDetailView place={toPreview(place)} />
      </div>
    </>
  );
}
