"use client";

import { ErrorView } from "@/shared/ui/StatusView";
import { SITE_NAME } from "@/shared/lib/site";
import "./tokens.css";
import "./globals.css";

// 루트 레이아웃이 깨졌을 때. 루트 레이아웃을 대신하므로 html·body와 전역 스타일을 직접 둔다.
// 글꼴(next/font)은 루트 레이아웃에서만 정의돼 여기서는 기본 글꼴로 그린다.
export default function GlobalError({ retry }: { error: Error & { digest?: string }; retry: () => void }) {
  return (
    <html lang="ko">
      <body>
        <title>{`불러오지 못했어요 — ${SITE_NAME}`}</title>
        <ErrorView retry={retry} />
      </body>
    </html>
  );
}
