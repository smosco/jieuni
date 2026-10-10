import type { AdminPick } from "./types";

export const WEEKDAY_LABEL: Record<AdminPick["weekday"], string> = {
  MON: "월",
  TUE: "화",
  WED: "수",
  THU: "목",
  FRI: "금",
};

const DAY = new Intl.DateTimeFormat("ko-KR", { timeZone: "UTC", month: "numeric", day: "numeric" });

// 서버가 준 날짜("2026-10-07")를 "10. 7."로 그리기만 한다. 시각이 없는 값이라 UTC 자정으로 읽어야 날이 밀리지 않는다.
export function dayLabel(date: string): string {
  return DAY.format(new Date(`${date}T00:00:00Z`));
}

export const LINE_MAX = 40;

// 편성 칸 저장 전 검사. 서버(PUT /api/admin/picks)와 같은 키·문구. 길이는 서버처럼 앞뒤 공백을 지운 뒤 센다.
export function pickErrors(placeId: number | null, line: string): Record<string, string> {
  const errors: Record<string, string> = {};
  if (placeId === null) errors.placeId = "공간을 골라 주세요";
  const trimmed = line.trim();
  if (trimmed === "") errors.line = "한 줄을 입력해 주세요";
  else if (trimmed.length > LINE_MAX) errors.line = `${LINE_MAX}자까지 쓸 수 있어요`;
  return errors;
}
