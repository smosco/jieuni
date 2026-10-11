"use client";

import { ErrorView } from "@/shared/ui/StatusView";

// 공개 화면이 백엔드를 부르다 실패하면 여기로 온다. 셸 레이아웃 안에서 그려진다.
export default function Error({ retry }: { error: Error & { digest?: string }; retry: () => void }) {
  return <ErrorView retry={retry} />;
}
