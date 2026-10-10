import { notFound } from "next/navigation";
import { AdminPlacePreview } from "@/features/admin/components/AdminPlacePreview";

export default async function AdminPlacePreviewPage({ params }: PageProps<"/admin/places/[id]/preview">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  return <AdminPlacePreview id={Number(id)} />;
}
