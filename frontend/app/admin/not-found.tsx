import { AdminNotFound } from "@/features/admin/components/AdminNotFound";

// /admin/places/abc 같은 notFound(). 관리자 레이아웃 안에서 그려진다.
export default function NotFound() {
  return <AdminNotFound />;
}
