"use client";

import { useEffect, useRef } from "react";

// 목록 끝의 빈 div가 화면 아래 400px 안에 들어오면 onReachEnd를 부른다.
// 받는 중에는 enabled를 끄고, 다 받으면 관찰을 새로 시작해 끝이 아직 보이면 바로 한 번 더 부른다.
export function useInfiniteScroll(onReachEnd: () => void, enabled: boolean) {
  const sentinel = useRef<HTMLDivElement>(null);
  const callback = useRef(onReachEnd);

  useEffect(() => {
    callback.current = onReachEnd;
  });

  useEffect(() => {
    const el = sentinel.current;
    if (!el || !enabled) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) callback.current();
      },
      { rootMargin: "0px 0px 400px 0px" },
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, [enabled]);

  return sentinel;
}
