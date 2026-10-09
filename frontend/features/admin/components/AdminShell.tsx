"use client";

import type { ReactNode } from "react";
import { useMe } from "@/features/me/queries";
import { AdminNav } from "./AdminNav";
import { AdminNotFound } from "./AdminNotFound";
import styles from "./Admin.module.css";

// 역할은 클라이언트에서 /api/me로 본다. 화면을 가리는 것뿐이고 실제 권한은 서버(/api/admin/** 403)가 지킨다.
// notFound()는 서버 컴포넌트 전용이라 404 화면을 직접 그린다.
export function AdminShell({ children }: { children: ReactNode }) {
  const { data: me, isPending, isError } = useMe();

  if (isPending) return null;
  // 401은 getMe가 null로 바꾸므로 여기 오지 않는다. 그 밖의 실패를 404로 보이면 권한이 없는 것처럼 오해한다.
  if (isError) {
    return (
      <div className={styles.notFound}>
        <p className={`body-md ${styles.notFoundText}`}>불러오지 못했어요. 다시 시도해 주세요</p>
      </div>
    );
  }
  if (me?.role !== "admin") return <AdminNotFound />;
  return (
    <div className={styles.shell}>
      <AdminNav />
      <main className={styles.main}>{children}</main>
    </div>
  );
}
