import type { SourceType } from "@/features/place/types";
import type { PhotoLicense } from "@/shared/api/types";
import type { AdminPlaceStatus } from "./types";

// 목록 필터 순서와 문구. 문구는 SPEC 관리자 화면의 상태 필터와 같다.
export const STATUS_ORDER: AdminPlaceStatus[] = ["suggested", "draft", "published", "paused", "closed", "rejected"];

export const STATUS_LABEL: Record<AdminPlaceStatus, string> = {
  suggested: "제보됨",
  draft: "작성 중",
  published: "공개",
  paused: "휴업",
  closed: "폐업",
  rejected: "반려",
};

export const SOURCE_TYPE_LABEL: Record<SourceType, string> = {
  article: "기사",
  video: "영상",
  instagram: "인스타그램",
  official: "공식 소개",
};

// 사진을 쓸 수 있는 근거. 직접 찍은 사진이 아니면 출처 링크가 필요하다.
export const PHOTO_LICENSE_LABEL: Record<PhotoLicense, string> = {
  own: "직접 찍음",
  press: "언론·공식 배포용",
  credited: "공개된 사진을 출처 표기로",
};

export function parseStatus(value: string | string[] | undefined): AdminPlaceStatus | null {
  return STATUS_ORDER.find((status) => status === value) ?? null;
}

// 상태·검색어를 주소에 남겨 새로고침·뒤로 가기에서 그대로 유지한다.
export function placesHref(status: AdminPlaceStatus | null, q: string): string {
  const params = new URLSearchParams();
  if (status) params.set("status", status);
  if (q) params.set("q", q);
  const query = params.toString();
  return query ? `/admin/places?${query}` : "/admin/places";
}

const UPDATED_AT = new Intl.DateTimeFormat("ko-KR", {
  timeZone: "Asia/Seoul",
  month: "numeric",
  day: "numeric",
  hour: "numeric",
  minute: "2-digit",
});

// 서버가 준 UTC 시각을 KST로 그리기만 한다.
export function updatedAtLabel(iso: string): string {
  return UPDATED_AT.format(new Date(iso));
}
