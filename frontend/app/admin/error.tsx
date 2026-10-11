"use client";

import { ErrorView } from "@/shared/ui/StatusView";

// 관리자 화면에서 난 오류. 관리자 레이아웃 안(메뉴 옆)에 그린다. 없으면 루트 error가 공개 셸로 덮는다.
export default function Error({ retry }: { error: Error & { digest?: string }; retry: () => void }) {
  return <ErrorView retry={retry} />;
}
