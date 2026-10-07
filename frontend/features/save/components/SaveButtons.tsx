"use client";

import styles from "./SaveButtons.module.css";

export type SaveStatus = "want" | "been";

const BUTTONS: { status: SaveStatus; label: string }[] = [
  { status: "want", label: "가고 싶어요" },
  { status: "been", label: "다녀왔어요" },
];

// 아직 로그인이 없어 비로그인 상태(둘 다 off)만 그린다. 누르면 로그인 시트, 저장 상태와 낙관적 업데이트는 로그인을 붙일 때 연결한다.
export function SaveButtons({ status = null, onPhoto = false }: { status?: SaveStatus | null; onPhoto?: boolean }) {
  return (
    <div className={styles.group}>
      {BUTTONS.map((button) => (
        <button
          key={button.status}
          type="button"
          className={`label-md ${styles.button} ${onPhoto ? styles.onPhoto : ""} ${status === button.status ? styles.on : ""}`}
          aria-pressed={status === button.status}
        >
          {button.label}
        </button>
      ))}
    </div>
  );
}
