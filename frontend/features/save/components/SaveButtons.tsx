"use client";

import { useRequireLogin } from "@/features/auth/useRequireLogin";
import type { SaveStatus } from "../types";
import styles from "./SaveButtons.module.css";

const BUTTONS: { status: SaveStatus; label: string }[] = [
  { status: "want", label: "가고 싶어요" },
  { status: "been", label: "다녀왔어요" },
];

// 지금은 누르면 저장만 보낸다(비로그인이면 로그인 시트). 켜진 상태 표시와 해제는 저장 상태를 붙일 때 연결한다.
export function SaveButtons({
  placeId,
  status = null,
  onPhoto = false,
  onLeave,
}: {
  placeId: number;
  status?: SaveStatus | null;
  onPhoto?: boolean;
  onLeave?: () => void;
}) {
  const requireLogin = useRequireLogin();

  return (
    <div className={styles.group}>
      {BUTTONS.map((button) => (
        <button
          key={button.status}
          type="button"
          className={`label-md ${styles.button} ${onPhoto ? styles.onPhoto : ""} ${status === button.status ? styles.on : ""}`}
          aria-pressed={status === button.status}
          onClick={() => requireLogin({ action: "save", placeId, status: button.status }, onLeave)}
        >
          {button.label}
        </button>
      ))}
    </div>
  );
}
