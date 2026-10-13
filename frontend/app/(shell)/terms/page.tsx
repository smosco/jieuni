import type { Metadata } from "next";
import { TermsView } from "@/features/info/components/TermsView";
import { pageMetadata, SITE_NAME } from "@/shared/lib/site";

export const metadata: Metadata = pageMetadata({
  title: `이용약관 — ${SITE_NAME}`,
  description: "공간의 지은이 이용약관",
  path: "/terms",
});

export default function TermsPage() {
  return <TermsView />;
}
