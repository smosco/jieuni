import type { FeedResponse, Weekday } from "./types";

const WEEKDAY_KO: Record<Weekday, string> = { MON: "월", TUE: "화", WED: "수", THU: "목", FRI: "금", SAT: "토", SUN: "일" };

export const WEEKDAY_EN: Record<Weekday, string> = {
  MON: "MONDAY", TUE: "TUESDAY", WED: "WEDNESDAY", THU: "THURSDAY", FRI: "FRIDAY", SAT: "SATURDAY", SUN: "SUNDAY",
};

export const WEEKDAYS: Weekday[] = ["MON", "TUE", "WED", "THU", "FRI"];

export function weekdayKo(weekday: Weekday): string {
  return `${WEEKDAY_KO[weekday]}요일`;
}

// 주 안의 자리 "03 / 05". 분모는 편성이 빈 날이 있어도 05로 고정이다.
export function weekPosition(weekday: Weekday): string {
  return `${String(WEEKDAYS.indexOf(weekday) + 1).padStart(2, "0")} / 05`;
}

// 마지막 화면의 제목·안내. 언제 열리는지는 서버가 정해 주고 여기서는 문구만 고른다.
export function endScreenCopy(
  nextRelative: FeedResponse["nextRelative"],
  nextWeekday: Weekday,
): { title: string; guide: string } {
  switch (nextRelative) {
    case "today":
      return { title: "오늘의 공간은 아직이에요.", guide: "오전 8시에 열려요." };
    case "tomorrow":
      return { title: "오늘은 여기까지예요.", guide: `내일은 ${weekdayKo(nextWeekday)}의 공간이 와요.` };
    case "later":
      return { title: "이번 주는 여기까지예요.", guide: "월요일 오전 8시에 새 공간이 와요." };
  }
}
