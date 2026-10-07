"use client";

import { useRouter } from "next/navigation";
import { cameFromInsideApp } from "@/shared/lib/entry";
import styles from "./BackButton.module.css";

// 앱 안에서 이동해 왔으면 뒤로, 이 화면으로 바로 들어왔으면(공유 링크 등) 홈으로.
export function BackButton({ onPhoto }: { onPhoto: boolean }) {
  const router = useRouter();

  function goBack() {
    if (cameFromInsideApp()) {
      router.back();
    } else {
      router.push("/");
    }
  }

  return (
    <button type="button" className={`${styles.button} ${onPhoto ? styles.onPhoto : ""}`} onClick={goBack} aria-label="뒤로 가기">
      <svg className={styles.icon} viewBox="0 0 24 24" fill="none" aria-hidden="true">
        <path d="M15 5l-7 7 7 7" stroke="currentColor" strokeLinecap="square" />
      </svg>
    </button>
  );
}
