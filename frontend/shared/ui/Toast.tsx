"use client";

import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from "react";
import styles from "./Toast.module.css";

const ToastContext = createContext<(message: string) => void>(() => {});

export function useToast() {
  return useContext(ToastContext);
}

// 한 번에 하나만. 새 안내가 이전 것을 바꾸고 2초 뒤 사라진다(안내가 쌓여 화면을 가리지 않게).
export function ToastProvider({ children }: { children: ReactNode }) {
  const [message, setMessage] = useState<string | null>(null);
  const timer = useRef<ReturnType<typeof setTimeout>>(undefined);

  const show = useCallback((next: string) => {
    clearTimeout(timer.current);
    setMessage(next);
    timer.current = setTimeout(() => setMessage(null), 2000);
  }, []);

  return (
    <ToastContext.Provider value={show}>
      {children}
      <div className={styles.region} aria-live="polite">
        {message && <p className={`label-md ${styles.toast}`}>{message}</p>}
      </div>
    </ToastContext.Provider>
  );
}
