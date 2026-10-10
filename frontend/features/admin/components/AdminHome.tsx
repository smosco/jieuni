"use client";

import Link from "next/link";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { WEEKDAY_LABEL, dayLabel } from "../pickSchema";
import { useAdminDashboard } from "../queries";
import type { AdminPick } from "../types";
import { AdminHead } from "./AdminHead";
import styles from "./Admin.module.css";

const USD = new Intl.NumberFormat("en-US", {
  style: "currency",
  currency: "USD",
  minimumFractionDigits: 2,
  maximumFractionDigits: 4,
});

export function AdminHome() {
  const { data, isError } = useAdminDashboard();

  return (
    <>
      <AdminHead title="관리자" />
      <div className={`label-md ${styles.shortcuts}`}>
        <Link href="/admin/places" className={styles.shortcut}>
          공간 목록 →
        </Link>
        <Link href="/admin/places/new" className={styles.shortcut}>
          새 공간 →
        </Link>
      </div>
      {isError && <p className={`body-md ${styles.empty}`}>불러오지 못했어요. 다시 시도해 주세요</p>}
      {data && (
        <div className={styles.dashboard}>
          <section className={styles.week}>
            <div className={styles.blockHead}>
              <SectionLabel>편성</SectionLabel>
              <Link href="/admin/picks" className={`label-md ${styles.shortcut}`}>
                편성하기 →
              </Link>
            </div>
            <div className={styles.weeks}>
              <WeekSummary title="이번 주" picks={data.picks.thisWeek} />
              <WeekSummary title="다음 주" picks={data.picks.nextWeek} />
            </div>
          </section>
          <dl className={styles.counts}>
            <div className={styles.count}>
              <dt className={`label-sm ${styles.meta}`}>검토 대기 제보</dt>
              <dd className="display-md">
                <Link href="/admin/suggestions">{data.pendingSuggestions}건</Link>
              </dd>
            </div>
            <div className={styles.count}>
              <dt className={`label-sm ${styles.meta}`}>작성 중인 공간</dt>
              <dd className="display-md">
                <Link href="/admin/places?status=draft">{data.draftCount}곳</Link>
              </dd>
            </div>
            <div className={styles.count}>
              <dt className={`label-sm ${styles.meta}`}>이번 달 AI 비용</dt>
              <dd className="display-md">{USD.format(data.llmCostThisMonthUsd)}</dd>
            </div>
          </dl>
        </div>
      )}
    </>
  );
}

function WeekSummary({ title, picks }: { title: string; picks: AdminPick[] }) {
  return (
    <div>
      <h3 className={`label-sm ${styles.weekTitle}`}>{title}</h3>
      <ul className={styles.summary}>
        {picks.map((pick) => (
          <li key={pick.date} className={styles.summaryRow}>
            <span className={`label-md ${styles.slotDay}`}>
              {WEEKDAY_LABEL[pick.weekday]} {dayLabel(pick.date)}
            </span>
            {pick.place ? (
              <span className={`body-md ${styles.summaryName}`}>{pick.place.name}</span>
            ) : (
              <span className={`body-md ${pick.locked ? styles.slotPast : styles.slotEmpty}`}>
                {pick.locked ? "편성 없이 지났어요" : "비어 있어요"}
              </span>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
