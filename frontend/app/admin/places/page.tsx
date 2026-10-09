import { AdminPlaceList } from "@/features/admin/components/AdminPlaceList";
import { parseStatus } from "@/features/admin/labels";

export default async function AdminPlacesPage({ searchParams }: PageProps<"/admin/places">) {
  const { status, q } = await searchParams;
  const query = typeof q === "string" ? q : "";
  // 조건이 바뀌면 검색칸을 새 주소의 값으로 다시 채운다.
  return <AdminPlaceList key={`${status}|${query}`} status={parseStatus(status)} q={query} />;
}
