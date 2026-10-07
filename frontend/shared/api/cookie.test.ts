import { describe, expect, it } from "vitest";
import { readCookie } from "./cookie";

describe("readCookie", () => {
  it("이름이 맞는 쿠키 값을 돌려준다", () => {
    expect(readCookie("SESSION=abc; XSRF-TOKEN=tok-1", "XSRF-TOKEN")).toBe("tok-1");
  });

  it("없으면 null", () => {
    expect(readCookie("SESSION=abc", "XSRF-TOKEN")).toBeNull();
    expect(readCookie("", "XSRF-TOKEN")).toBeNull();
  });

  it("값 안의 =와 퍼센트 인코딩을 다룬다", () => {
    expect(readCookie("XSRF-TOKEN=a%3Db=c", "XSRF-TOKEN")).toBe("a=b=c");
  });
});
