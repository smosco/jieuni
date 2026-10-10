import type { SourceType } from "./types";

// 코드 값 → 상세 화면 문구
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
