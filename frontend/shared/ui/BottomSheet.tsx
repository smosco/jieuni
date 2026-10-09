"use client";

import { useEffect, useRef, type ReactNode } from "react";
import styles from "./BottomSheet.module.css";

// 네이티브 <dialog>의 모달: Esc로 닫기, 포커스 가두기, 뒤 화면 비활성화를 브라우저가 한다.
// 딤을 누르면 닫는다. 안쪽 내용은 .body가 채우므로 dialog 자체가 눌린 것은 딤뿐이다.
export function BottomSheet({
  open,
  onClose,
  labelledBy,
  children,
}: {
  open: boolean;
  onClose: () => void;
  labelledBy: string;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      ref={ref}
      className={styles.sheet}
      aria-labelledby={labelledBy}
      onClose={onClose}
      onClick={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div className={styles.body}>
        <span className={styles.handle} aria-hidden="true" />
        {children}
      </div>
    </dialog>
  );
}
