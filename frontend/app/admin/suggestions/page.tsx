import { AdminSuggestions } from "@/features/admin/components/AdminSuggestions";

export default async function AdminSuggestionsPage({ searchParams }: PageProps<"/admin/suggestions">) {
  const { type } = await searchParams;
  return <AdminSuggestions type={type === "credits" ? "credits" : "places"} />;
}
