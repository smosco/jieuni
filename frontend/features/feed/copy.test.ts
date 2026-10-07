import { describe, expect, it } from "vitest";
import { endScreenCopy, weekPosition } from "./copy";

describe("endScreenCopy", () => {
  it("평일 8시 전이면 오늘 8시에 열린다고 말한다", () => {
    expect(endScreenCopy("today", "WED")).toEqual({ title: "오늘의 공간은 아직이에요.", guide: "오전 8시에 열려요." });
  });

  it("다음 공개가 내일이면 그 요일을 말한다", () => {
    expect(endScreenCopy("tomorrow", "THU")).toEqual({ title: "오늘은 여기까지예요.", guide: "내일은 목요일의 공간이 와요." });
    expect(endScreenCopy("tomorrow", "MON").guide).toBe("내일은 월요일의 공간이 와요.");
  });

  it("금요일 8시 뒤·토요일은 월요일을 말한다", () => {
    expect(endScreenCopy("later", "MON")).toEqual({ title: "이번 주는 여기까지예요.", guide: "월요일 오전 8시에 새 공간이 와요." });
  });
});

describe("weekPosition", () => {
  it("월 01 ~ 금 05, 분모는 05 고정", () => {
    expect(weekPosition("MON")).toBe("01 / 05");
    expect(weekPosition("WED")).toBe("03 / 05");
    expect(weekPosition("FRI")).toBe("05 / 05");
  });
});
