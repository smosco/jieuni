"use client";

import { usePathname } from "next/navigation";
import { useEffect, type ReactNode } from "react";
import { useMe } from "@/features/me/queries";
import { AdminNav } from "./AdminNav";
import { AdminNotFound } from "./AdminNotFound";
import { LeaveGuardProvider } from "./LeaveGuard";
import styles from "./Admin.module.css";

const ADMIN_TITLE = "관리자 — 공간의 지은이";

// 역할은 클라이언트에서 /api/me로 본다. 화면을 가리는 것뿐이고 실제 권한은 서버(/api/admin/** 403)가 지킨다.
// notFound()는 서버 컴포넌트 전용이라 404 화면을 직접 그린다.
export function AdminShell({ children }: { children: ReactNode }) {
  const { data: me, isPending, isError } = useMe();
  const isAdmin = me?.role === "admin";
  const pathname = usePathname();

  // 서버 metadata 제목은 "공간의 지은이"다. 관리자로 확인된 뒤에만 탭 제목을 바꾼다. 화면을 옮길 때마다 다시 둔다.
  useEffect(() => {
    if (isAdmin) document.title = ADMIN_TITLE;
  }, [isAdmin, pathname]);

  if (isPending) return null;
  // 401은 getMe가 null로 바꾸므로 여기 오지 않는다. 그 밖의 실패를 404로 보이면 권한이 없는 것처럼 오해한다.
  if (isError) {
    return (
      <div className={styles.notFound}>
        <p className={`body-md ${styles.notFoundText}`}>불러오지 못했어요. 다시 시도해 주세요</p>
      </div>
    );
  }
  if (!isAdmin) return <AdminNotFound />;
  return (
    <LeaveGuardProvider>
      <div className={styles.shell}>
        <AdminNav />
        <main className={styles.main}>{children}</main>
      </div>
    </LeaveGuardProvider>
  );
}
