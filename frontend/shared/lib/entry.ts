"use client";

import { useEffect } from "react";

// 이 탭에서 문서가 처음 열린 경로. 클라이언트 이동은 루트 레이아웃을 다시 마운트하지 않으므로
// 이 값은 새로고침·외부 링크 진입 때만 바뀐다. 뒤로 가기가 앱 밖으로 나가지 않게 하는 데 쓴다.
const KEY = "jieuni:entry-path";

export function EntryPathRecorder() {
  useEffect(() => {
    sessionStorage.setItem(KEY, window.location.pathname);
  }, []);
  return null;
}

// 지금 화면이 이 문서의 첫 화면이 아니면 앱 안에서 이동해 온 것이다.
export function cameFromInsideApp(): boolean {
  return sessionStorage.getItem(KEY) !== window.location.pathname;
}
