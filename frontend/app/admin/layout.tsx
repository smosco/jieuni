import type { Metadata } from "next";
import { AdminShell } from "@/features/admin/components/AdminShell";

// 공개 셸((shell) 그룹, 탭 바) 밖에 둔다. 검색에 걸리지 않게 한다.
export const metadata: Metadata = {
  // 일반 사용자에게 관리자 화면이 있다는 것을 드러내지 않는다. 관리자 제목은 AdminShell이 역할을 확인한 뒤 바꾼다.
  title: "공간의 지은이",
  robots: { index: false, follow: false },
};

export default function AdminLayout({ children }: LayoutProps<"/admin">) {
  return <AdminShell>{children}</AdminShell>;
}
