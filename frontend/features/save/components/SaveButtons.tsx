"use client";

import styles from "./SaveButtons.module.css";

export type SaveStatus = "want" | "been";

const BUTTONS: { status: SaveStatus; label: string }[] = [
  { status: "want", label: "가고 싶어요" },
  { status: "been", label: "다녀왔어요" },
];

// DESIGN 4절 SaveButton. 이 조각에서는 비로그인 상태(둘 다 off)만 그린다.
// 누르면 로그인 시트(F6), 저장 상태와 낙관적 업데이트는 F7에서 붙인다.
export function SaveButtons({ status = null }: { status?: SaveStatus | null }) {
  return (
    <div className={styles.group}>
      {BUTTONS.map((button) => (
        <button
          key={button.status}
          type="button"
          className={`label-md ${styles.button} ${status === button.status ? styles.on : ""}`}
          aria-pressed={status === button.status}
        >
          {button.label}
        </button>
      ))}
    </div>
  );
}
