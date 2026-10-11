"use client";

import { ErrorView } from "@/shared/ui/StatusView";
import { Shell } from "@/shared/ui/Shell";

// 셸 그룹 밖(관리자 레이아웃 등)에서 난 오류. 셸 레이아웃이 없으니 셸을 직접 그린다.
export default function Error({ retry }: { error: Error & { digest?: string }; retry: () => void }) {
  return (
    <Shell>
      <ErrorView retry={retry} />
    </Shell>
  );
}
