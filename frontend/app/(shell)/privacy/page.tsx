import type { Metadata } from "next";
import { PrivacyView } from "@/features/info/components/PrivacyView";
import { pageMetadata, SITE_NAME } from "@/shared/lib/site";

export const metadata: Metadata = pageMetadata({
  title: `개인정보처리방침 — ${SITE_NAME}`,
  description: "공간의 지은이가 받는 개인정보와 처리 방법",
  path: "/privacy",
});

export default function PrivacyPage() {
  return <PrivacyView />;
}
