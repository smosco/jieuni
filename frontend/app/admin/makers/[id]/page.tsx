import { notFound } from "next/navigation";
import { AdminMakerForm } from "@/features/admin/components/AdminMakerForm";

export default async function AdminMakerPage({ params }: PageProps<"/admin/makers/[id]">) {
  const { id } = await params;
  if (!/^\d+$/.test(id)) notFound();
  return <AdminMakerForm id={Number(id)} />;
}
