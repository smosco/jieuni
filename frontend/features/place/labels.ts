import type { Access, SourceType } from "./types";

// DESIGN.md 5절 04 문구 표
export const ACCESS_LABEL: Record<Access, string> = {
  walk_in: "바로 방문",
  reservation: "예약",
  ticket: "입장권",
  guests_only: "투숙객만",
};

export const ENCOUNTER_LABEL: Record<SourceType, string> = {
  article: "기사로 만남",
  video: "영상으로 만남",
  instagram: "인스타그램으로 만남",
  official: "공식 소개로 만남",
};

// "2026-05" → "2026년 5월"
export function yearMonth(value: string): string {
  const [year, month] = value.split("-");
  return `${year}년 ${Number(month)}월`;
}
