import { describe, expect, it } from "vitest";
import { isMakerDirty, makerSchema, toMakerValues, type MakerFormValues } from "./makerSchema";
import { toFieldErrors } from "./placeSchema";
import type { AdminMaker } from "./types";

const MAKER: AdminMaker = {
  id: 2,
  slug: "seojin-yoon",
  name: "윤서진",
  kind: "person",
  region: "서울 종로구",
  intro: null,
  website: null,
  instagram: "seojin.yoon",
  affiliation: { id: 1, slug: "atelier-on", name: "아틀리에 온" },
  places: [],
  slugLocked: true,
  createdAt: "2026-10-01T00:00:00Z",
  updatedAt: "2026-10-01T00:00:00Z",
};

function errors(values: Partial<MakerFormValues>) {
  const result = makerSchema.safeParse({ ...toMakerValues(MAKER), ...values });
  return result.success ? {} : toFieldErrors(result.error.issues);
}

describe("만든 사람 검증", () => {
  it("저장 본문: 빈 칸은 null, 인스타그램 앞의 @는 지운다", () => {
    expect(makerSchema.parse({ ...toMakerValues(MAKER), region: "  ", instagram: " @seojin.yoon " })).toEqual({
      slug: "seojin-yoon",
      name: "윤서진",
      kind: "person",
      region: null,
      intro: null,
      website: null,
      instagram: "seojin.yoon",
      affiliationId: 1,
    });
  });

  it("서버와 같은 문구", () => {
    expect(
      errors({
        name: " ",
        kind: "",
        slug: "Seojin",
        region: "가".repeat(51),
        intro: "가".repeat(201),
        website: "javascript:x",
        instagram: "seo jin",
      }),
    ).toEqual({
      name: "이름을 입력해 주세요",
      kind: "종류를 골라 주세요",
      slug: "영문 소문자·숫자·하이픈만 쓸 수 있어요",
      region: "지역은 50자까지 쓸 수 있어요",
      intro: "소개는 200자까지 쓸 수 있어요",
      website: "https://로 시작하는 주소를 넣어 주세요",
      instagram: "인스타그램 아이디만 넣어 주세요",
    });
  });

  it("소속 이름을 고쳤으면 목록에서 골라야 한다", () => {
    expect(errors({ affiliation: { id: null, name: "아틀리" } })).toEqual({ affiliationId: "목록에서 골라 주세요" });
    expect(errors({ affiliation: { id: null, name: "" } })).toEqual({});
  });
});

describe("만든 사람 변경 판단", () => {
  it("불러온 그대로·공백·@만 다르면 변경 아님", () => {
    expect(isMakerDirty(toMakerValues(MAKER), MAKER)).toBe(false);
    expect(isMakerDirty({ ...toMakerValues(MAKER), name: " 윤서진 ", instagram: "@seojin.yoon" }, MAKER)).toBe(false);
  });

  it("소속을 비우거나 지역을 바꾸면 변경", () => {
    expect(isMakerDirty({ ...toMakerValues(MAKER), affiliation: { id: null, name: "" } }, MAKER)).toBe(true);
    expect(isMakerDirty({ ...toMakerValues(MAKER), region: "서울 성북구" }, MAKER)).toBe(true);
  });
});
