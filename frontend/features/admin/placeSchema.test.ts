import { describe, expect, it } from "vitest";
import { draftSchema, publishSchema, toFieldErrors, type PlaceFormValues, type PublishInput } from "./placeSchema";
import type { AdminPhoto } from "./types";

const EMPTY: PlaceFormValues = {
  slug: "",
  name: "이름",
  kind: "",
  region: "",
  address: "",
  lat: "",
  lng: "",
  access: "",
  visitNote: "",
  summary: "",
  impression: "",
  senses: { light: "", sound: "", scent: "", touch: "" },
  essay: "",
  visitedByEditorOn: "",
};

function draftErrors(values: Partial<PlaceFormValues>) {
  const result = draftSchema.safeParse({ ...EMPTY, ...values });
  return result.success ? {} : toFieldErrors(result.error.issues);
}

const PHOTO: AdminPhoto = {
  id: 1,
  objectKey: "places/1/a",
  position: 0,
  url: { w1600: "a", w800: "b" },
  width: 1600,
  height: 1200,
  alt: "마당",
  credit: "지은이",
  license: "own",
  sourceUrl: null,
};

const PUBLISHABLE: PublishInput = {
  slug: "maru",
  name: "마루",
  kind: "cafe",
  region: "경기 양평군",
  address: "주소",
  lat: 37.5,
  lng: 127.5,
  access: "walk_in",
  visitNote: null,
  summary: "한 줄",
  impression: "감상",
  senses: { light: null, sound: null, scent: null, touch: null },
  essay: null,
  visitedByEditorOn: null,
  credits: [
    {
      position: 0,
      role: "architecture",
      verified: false,
      sourceUrl: "https://example.com",
      maker: { id: 3, slug: null, name: "스튜디오", kind: "studio" },
      makerId: 3,
    },
  ],
  sources: [{ title: "기사", url: "https://example.com/a", type: "article" }],
  photos: [PHOTO],
  embeds: [],
};

function publishErrors(place: Partial<PublishInput>) {
  const result = publishSchema.safeParse({ ...PUBLISHABLE, ...place });
  return result.success ? {} : toFieldErrors(result.error.issues);
}

describe("임시저장 형식 검증", () => {
  it("빈 칸은 null로, 앞뒤 공백은 지워서 보낸다", () => {
    const result = draftSchema.parse({ ...EMPTY, name: "  마루 ", summary: "   ", lat: " 37.5 ", kind: "cafe" });
    expect(result).toMatchObject({ name: "마루", summary: null, lat: 37.5, kind: "cafe", slug: null, access: null });
    expect(result.senses).toEqual({ light: null, sound: null, scent: null, touch: null });
  });

  it.each([
    ["slug", "a".repeat(81), "주소(slug)는 80자까지 쓸 수 있어요"],
    ["name", "a".repeat(101), "공간 이름은 100자까지 쓸 수 있어요"],
    ["region", "a".repeat(51), "지역은 50자까지 쓸 수 있어요"],
    ["address", "a".repeat(201), "주소는 200자까지 쓸 수 있어요"],
    ["visitNote", "a".repeat(101), "100자까지 쓸 수 있어요"],
    ["summary", "a".repeat(61), "60자까지 쓸 수 있어요"],
    ["impression", "a".repeat(401), "400자까지 쓸 수 있어요"],
  ] as const)("%s 길이 초과", (key, value, message) => {
    expect(draftErrors({ [key]: value })[key]).toBe(message);
  });

  it("감각은 칸마다 60자, 키는 senses.칸 이름", () => {
    const errors = draftErrors({ senses: { ...EMPTY.senses, touch: "a".repeat(61) } });
    expect(errors).toEqual({ "senses.touch": "60자까지 쓸 수 있어요" });
  });

  it("길이는 앞뒤 공백을 지운 뒤에 잰다", () => {
    expect(draftErrors({ summary: ` ${"a".repeat(60)} ` })).toEqual({});
  });

  it("에세이는 길이 제한이 없다", () => {
    expect(draftErrors({ essay: "a".repeat(10000) })).toEqual({});
  });

  it("이름은 비울 수 없다", () => {
    expect(draftErrors({ name: "  " })).toEqual({ name: "공간 이름을 입력해 주세요" });
  });

  it.each(["Maru", "ma ru", "-maru", "maru-", "ma--ru", "마루"])("slug 형식 %s", (slug) => {
    expect(draftErrors({ slug })).toEqual({ slug: "영문 소문자·숫자·하이픈만 쓸 수 있어요" });
  });

  it("slug는 소문자·숫자·하이픈이면 통과", () => {
    expect(draftErrors({ slug: "maru-2" })).toEqual({});
  });

  it("좌표는 숫자이고 범위 안", () => {
    expect(draftErrors({ lat: "abc", lng: "181" })).toEqual({
      lat: "숫자로 입력해 주세요",
      lng: "경도는 -180에서 180 사이예요",
    });
    expect(draftErrors({ lat: "-90.1" })).toEqual({ lat: "위도는 -90에서 90 사이예요" });
    expect(draftErrors({ lat: "-90", lng: "180" })).toEqual({});
  });

  it("머물러 본 달은 년-월", () => {
    expect(draftErrors({ visitedByEditorOn: "2026-13" })).toEqual({
      visitedByEditorOn: "2026-05처럼 년-월로 입력해 주세요",
    });
    expect(draftSchema.parse({ ...EMPTY, visitedByEditorOn: "2026-05" }).visitedByEditorOn).toBe("2026-05");
  });
});

describe("공개 시 검증", () => {
  it("모두 갖추면 통과", () => {
    expect(publishErrors({})).toEqual({});
  });

  it.each([
    ["slug", "주소(slug)를 정해 주세요"],
    ["name", "공간 이름을 입력해 주세요"],
    ["kind", "종류를 골라 주세요"],
    ["region", "지역을 입력해 주세요"],
    ["address", "주소를 입력해 주세요"],
    ["lat", "좌표를 입력해 주세요"],
    ["lng", "좌표를 입력해 주세요"],
    ["access", "방문 방식을 골라 주세요"],
    ["summary", "한 줄 소개를 입력해 주세요"],
    ["impression", "감상을 입력해 주세요"],
  ] as const)("%s 없음", (key, message) => {
    expect(publishErrors({ [key]: null })).toEqual({ [key]: message });
  });

  it("공백뿐인 문자열도 없음", () => {
    expect(publishErrors({ name: "  " })).toEqual({ name: "공간 이름을 입력해 주세요" });
  });

  it("출처 있는 크레딧이 필요하고, 확인 여부는 보지 않는다", () => {
    const credit = { ...PUBLISHABLE.credits[0], sourceUrl: null, verified: true };
    expect(publishErrors({ credits: [credit] })).toEqual({ credits: "출처가 있는 크레딧이 1개 이상 필요해요" });
    expect(publishErrors({ credits: [] })).toEqual({ credits: "출처가 있는 크레딧이 1개 이상 필요해요" });
  });

  it("머물러 본 날이 없으면 출처가 필요하다", () => {
    expect(publishErrors({ sources: [] })).toEqual({ sources: "머물러 본 날이 없으면 출처가 1개 이상 필요해요" });
    expect(publishErrors({ sources: [], visitedByEditorOn: "2026-05" })).toEqual({});
  });

  it("사진 또는 임베드가 필요하다", () => {
    expect(publishErrors({ photos: [] })).toEqual({ photos: "사진 또는 임베드가 1개 이상 필요해요" });
    expect(publishErrors({ photos: [], embeds: [{ type: "instagram", url: "https://instagram.com/p/x" }] })).toEqual(
      {},
    );
  });

  it("모든 사진에 설명·출처 이름, own이 아니면 출처 링크", () => {
    const photos = [
      { ...PHOTO, alt: "", credit: " " },
      { ...PHOTO, license: "press" as const, sourceUrl: null },
      { ...PHOTO, license: "own" as const, sourceUrl: null },
    ];
    expect(publishErrors({ photos })).toEqual({
      "photos[0].alt": "사진 설명을 입력해 주세요",
      "photos[0].credit": "사진 출처(이름)를 입력해 주세요",
      "photos[1].sourceUrl": "출처 링크를 입력해 주세요",
    });
  });

  it("여러 개가 함께 실패하면 서버와 같은 순서", () => {
    const errors = publishErrors({ address: null, lat: null, credits: [] });
    expect(Object.keys(errors)).toEqual(["address", "lat", "credits"]);
  });
});
