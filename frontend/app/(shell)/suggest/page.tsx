import type { Metadata } from "next";
import { SuggestPlace } from "@/features/suggestion/components/SuggestPlace";

export const metadata: Metadata = { title: "공간 소개하기 — 공간의 지은이" };

export default function SuggestPage() {
  return <SuggestPlace />;
}
