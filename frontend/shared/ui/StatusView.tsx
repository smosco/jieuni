"use client";

import Link from "next/link";
import { SITE_NAME } from "@/shared/lib/site";
import styles from "./StatusView.module.css";

// 404·오류처럼 화면 전체가 한 가지 상태를 알릴 때. 셸 안(셸 그룹의 not-found·error)과 밖(없는 주소)에서 같이 쓴다.
function StatusView({ label, title, text, children }: { label: string; title: string; text: string; children: React.ReactNode }) {
  return (
    <main className={styles.status}>
      <p className={`label-sm ${styles.label}`}>{label}</p>
      <h1 className={`display-lg ${styles.title}`}>{title}</h1>
      <p className={`body-md ${styles.text}`}>{text}</p>
      <div className={`label-md ${styles.actions}`}>{children}</div>
    </main>
  );
}

// not-found에는 metadata 내보내기가 없어 제목은 <title>로 둔다(React가 head로 올린다).
export function NotFoundView() {
  return (
    <>
      <title>{`찾을 수 없는 페이지 — ${SITE_NAME}`}</title>
      <StatusView label="NOT FOUND" title="찾을 수 없는 페이지예요" text="주소가 맞는지 한 번 더 확인해 주세요.">
        <Link href="/" className={styles.action}>
          오늘의 공간 →
        </Link>
        <Link href="/places" className={styles.action}>
          둘러보기 →
        </Link>
      </StatusView>
    </>
  );
}

// retry는 데이터를 다시 받아 그 구간을 다시 그린다.
export function ErrorView({ retry }: { retry: () => void }) {
  return (
    <StatusView label="ERROR" title="불러오지 못했어요" text="잠시 뒤에 다시 시도해 주세요.">
      <button type="button" className={styles.action} onClick={() => retry()}>
        다시 시도
      </button>
      <Link href="/" className={styles.action}>
        오늘의 공간 →
      </Link>
    </StatusView>
  );
}
