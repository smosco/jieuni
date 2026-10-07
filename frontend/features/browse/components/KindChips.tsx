import Link from "next/link";
import type { PlaceKind } from "@/shared/api/types";
import { KIND_LABEL } from "@/shared/lib/labels";
import { KIND_ORDER, placesHref, toggleKind } from "../kinds";
import styles from "./Browse.module.css";

// 칩은 링크다. 조건이 URL에 남아 공유·뒤로 가기·새로고침에서 그대로 유지된다.
export function KindChips({ selected }: { selected: PlaceKind[] }) {
  return (
    <div className={`label-sm ${styles.chips}`}>
      <Chip href={placesHref([])} on={selected.length === 0} label="전체" />
      {KIND_ORDER.map((kind) => (
        <Chip key={kind} href={placesHref(toggleKind(selected, kind))} on={selected.includes(kind)} label={KIND_LABEL[kind]} />
      ))}
    </div>
  );
}

function Chip({ href, on, label }: { href: string; on: boolean; label: string }) {
  return (
    <Link href={href} scroll={false} className={`${styles.chip} ${on ? styles.chipOn : ""}`} aria-current={on ? "true" : undefined}>
      {label}
    </Link>
  );
}
