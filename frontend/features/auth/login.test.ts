import { describe, expect, it } from "vitest";
import { PENDING_TTL_MS, loginUrl, parsePending, pathOf, stripLoginError } from "./login";

describe("loginUrl", () => {
  it("경로와 쿼리를 redirect에 인코딩해 넣는다", () => {
    expect(loginUrl("/places/a", "?x=1&y=2")).toBe("/oauth2/authorization/google?redirect=%2Fplaces%2Fa%3Fx%3D1%26y%3D2");
  });

  it("한글 경로도 그대로 인코딩한다", () => {
    expect(loginUrl("/places/카페", "")).toBe(`/oauth2/authorization/google?redirect=${encodeURIComponent("/places/카페")}`);
  });
});

describe("stripLoginError", () => {
  it("login만 빼고 나머지 쿼리는 남긴다", () => {
    expect(stripLoginError("?kind=cafe&login=error")).toBe("?kind=cafe");
  });

  it("login만 있었으면 빈 문자열", () => {
    expect(stripLoginError("?login=error")).toBe("");
  });
});

describe("parsePending", () => {
  const now = 1_000_000_000;
  const save = { action: "save", placeId: 3, status: "want", returnTo: "/", savedAt: now - 1000 };

  it("모양이 맞으면 돌려준다", () => {
    expect(parsePending(JSON.stringify(save), now)).toEqual(save);
    expect(parsePending(JSON.stringify({ action: "follow", makerId: 7, returnTo: "/makers/a", savedAt: now }), now)).toEqual({
      action: "follow",
      makerId: 7,
      returnTo: "/makers/a",
      savedAt: now,
    });
  });

  it("10분이 지났으면 null", () => {
    expect(parsePending(JSON.stringify({ ...save, savedAt: now - PENDING_TTL_MS }), now)).not.toBeNull();
    expect(parsePending(JSON.stringify({ ...save, savedAt: now - PENDING_TTL_MS - 1 }), now)).toBeNull();
  });

  it("저장 시각이 미래거나 없으면 null", () => {
    expect(parsePending(JSON.stringify({ ...save, savedAt: now + 1 }), now)).toBeNull();
    expect(parsePending(JSON.stringify({ ...save, savedAt: undefined }), now)).toBeNull();
  });

  it("없거나 깨졌거나 모양이 어긋나면 null", () => {
    expect(parsePending(null, now)).toBeNull();
    expect(parsePending("{", now)).toBeNull();
    expect(parsePending(JSON.stringify({ ...save, status: "liked" }), now)).toBeNull();
    expect(parsePending(JSON.stringify({ ...save, action: "delete" }), now)).toBeNull();
    expect(parsePending(JSON.stringify({ action: "follow", makerId: "7", returnTo: "/", savedAt: now }), now)).toBeNull();
  });
});

describe("pathOf", () => {
  it("쿼리를 뺀 경로", () => {
    expect(pathOf("/places/a?x=1")).toBe("/places/a");
    expect(pathOf("/")).toBe("/");
  });
});
