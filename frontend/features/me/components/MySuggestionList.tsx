"use client";

import { useId, useState } from "react";
import { ROLE_LABEL } from "@/shared/lib/labels";
import { BottomSheet } from "@/shared/ui/BottomSheet";
import { useMySuggestions } from "../queries";
import type { MySuggestions } from "../types";
import styles from "./MySpace.module.css";

// 공간 제보는 공간의 상태를 그대로 받는다. 관리자가 초안으로 옮겨 작성 중이어도 검토 중이다.
const PLACE_STATUS: Record<MySuggestions["places"][number]["status"], string> = {
  suggested: "검토 중",
  draft: "검토 중",
  published: "소개됨",
  paused: "소개됨",
  closed: "소개됨",
  rejected: "보류",
};

const CREDIT_STATUS: Record<MySuggestions["credits"][number]["status"], string> = {
  pending: "검토 중",
  accepted: "소개됨",
  declined: "보류",
};

// 탭 목록은 끝없이 이어지므로 아래에 두면 닿을 수 없다. 탭 위의 한 줄 링크로 두고 시트로 연다.
// 제보가 없으면 링크를 그리지 않는다. 개수는 보이지 않는다.
export function MySuggestionsLink() {
  const { data } = useMySuggestions();
  const [open, setOpen] = useState(false);
  const titleId = useId();
  if (!data || data.places.length + data.credits.length === 0) return null;

  return (
    <>
      <button type="button" className={styles.headLink} onClick={() => setOpen(true)}>
        내가 소개한 공간 →
      </button>
      <BottomSheet open={open} onClose={() => setOpen(false)} labelledBy={titleId}>
        <h2 id={titleId} className={`display-md ${styles.sheetTitle}`}>
          내가 소개한 공간
        </h2>
        <MySuggestionList data={data} />
      </BottomSheet>
    </>
  );
}

function MySuggestionList({ data }: { data: MySuggestions }) {
  return (
    <ul className={`${styles.rows} ${styles.sheetRows}`}>
      {data.places.map((place) => (
        <li key={`place-${place.id}`} className={styles.row}>
          <span className={styles.rowText}>
            <span className="body-md">{place.name}</span>
            <span className={`label-sm ${styles.sub}`}>{place.region}</span>
          </span>
          <span className={`label-sm ${styles.status}`}>{PLACE_STATUS[place.status]}</span>
        </li>
      ))}
      {data.credits.map((credit) => (
        <li key={`credit-${credit.id}`} className={styles.row}>
          <span className={styles.rowText}>
            <span className="body-md">{credit.placeName}</span>
            <span className={`label-sm ${styles.sub}`}>
              크레딧 · {ROLE_LABEL[credit.role]} {credit.makerName}
            </span>
          </span>
          <span className={`label-sm ${styles.status}`}>{CREDIT_STATUS[credit.status]}</span>
        </li>
      ))}
    </ul>
  );
}
