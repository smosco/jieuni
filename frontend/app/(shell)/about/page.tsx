import type { Metadata } from "next";
import { AboutView } from "@/features/info/components/AboutView";
import { pageMetadata, SITE_DESCRIPTION, SITE_NAME } from "@/shared/lib/site";

export const metadata: Metadata = pageMetadata({
  title: `소개 — ${SITE_NAME}`,
  description: SITE_DESCRIPTION,
  path: "/about",
});

export default function AboutPage() {
  return <AboutView />;
}
