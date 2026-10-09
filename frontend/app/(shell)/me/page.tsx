import type { Metadata } from "next";
import { MySpace, type MyTab } from "@/features/me/components/MySpace";

export const metadata: Metadata = { title: "내 공간 — 공간의 지은이" };

// 탭마다 주소를 달리해(/me, ?tab=been, ?tab=following) 뒤로 가기로 돌아갈 수 있게 한다.
function parseTab(value: string | string[] | undefined): MyTab {
  return value === "been" || value === "following" ? value : "want";
}

export default async function MePage({ searchParams }: PageProps<"/me">) {
  return <MySpace tab={parseTab((await searchParams).tab)} />;
}
