"use client";

import { useMeStateLoader } from "../queries";

// 서버 컴포넌트 화면(상세·프로필)이 그 화면의 id를 넘겨 저장·팔로우 상태를 채운다.
export function MeStateLoader({ places = [], makers = [] }: { places?: number[]; makers?: number[] }) {
  useMeStateLoader(places, makers);
  return null;
}
