import { describe, expect, it } from "vitest";
import { nextSaveStatus } from "./types";

describe("nextSaveStatus", () => {
  it("꺼진 상태에서 누르면 그 상태", () => {
    expect(nextSaveStatus(null, "want")).toBe("want");
    expect(nextSaveStatus(null, "been")).toBe("been");
  });

  it("켜진 버튼을 다시 누르면 해제", () => {
    expect(nextSaveStatus("want", "want")).toBeNull();
    expect(nextSaveStatus("been", "been")).toBeNull();
  });

  it("다른 버튼을 누르면 그쪽으로 바뀐다", () => {
    expect(nextSaveStatus("want", "been")).toBe("been");
    expect(nextSaveStatus("been", "want")).toBe("want");
  });
});
