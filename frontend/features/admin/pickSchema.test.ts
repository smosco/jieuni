import { describe, expect, it } from "vitest";
import { dayLabel, pickErrors } from "./pickSchema";

describe("편성 칸 날짜", () => {
  it("날짜만 있는 값을 그 날 그대로 그린다", () => {
    expect(dayLabel("2026-10-05")).toBe("10. 5.");
    expect(dayLabel("2026-12-31")).toBe("12. 31.");
  });
});

describe("편성 칸 검사", () => {
  it("공간과 한 줄이 있어야 한다", () => {
    expect(pickErrors(null, "  ")).toEqual({ placeId: "공간을 골라 주세요", line: "한 줄을 입력해 주세요" });
    expect(pickErrors(1, "빛이 머무는 정원")).toEqual({});
  });

  it("한 줄은 앞뒤 공백을 빼고 40자까지", () => {
    expect(pickErrors(1, ` ${"가".repeat(40)} `)).toEqual({});
    expect(pickErrors(1, "가".repeat(41))).toEqual({ line: "40자까지 쓸 수 있어요" });
  });
});
