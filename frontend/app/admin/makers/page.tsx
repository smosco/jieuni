import { AdminMakerList } from "@/features/admin/components/AdminMakerList";

export default async function AdminMakersPage({ searchParams }: PageProps<"/admin/makers">) {
  const { q } = await searchParams;
  const query = typeof q === "string" ? q : "";
  // 검색어가 바뀌면 검색칸을 새 주소의 값으로 다시 채운다.
  return <AdminMakerList key={query} q={query} />;
}
