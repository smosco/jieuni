import { notFound } from "next/navigation";
import { AdminPlaceForm } from "@/features/admin/components/AdminPlaceForm";

export default async function AdminPlacePage({ params }: PageProps<"/admin/places/[id]">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  return <AdminPlaceForm id={Number(id)} />;
}
