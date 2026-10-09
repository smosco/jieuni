import type { Metadata } from "next";
import { AdminShell } from "@/features/admin/components/AdminShell";

// 공개 셸((shell) 그룹, 탭 바) 밖에 둔다. 검색에 걸리지 않게 한다.
export const metadata: Metadata = {
  title: "관리자 — 공간의 지은이",
  robots: { index: false, follow: false },
};

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return <AdminShell>{children}</AdminShell>;
}
