import { describe, expect, it } from "vitest";
import {
  draftSchema,
  hintText,
  isDirty,
  marksVisible,
  photoPublishFields,
  publishMissingCount,
  publishErrors as formPublishErrors,
  publishSchema,
  saveLabel,
  toFieldErrors,
  toFormValues,
  type PlaceFormValues,
  type PublishInput,
} from "./placeSchema";
import type { AdminPhoto, AdminPlace } from "./types";

const EMPTY: PlaceFormValues = {
  slug: "",
  name: "이름",
  kind: "",
  region: "",
  address: "",
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
    const result = draftSchema.parse({ ...EMPTY, name: "  마루 ", summary: "   ", kind: "cafe" });
    expect(result).toMatchObject({ name: "마루", summary: null, kind: "cafe", slug: null, access: null });
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
    expect(draftErrors({ name: "  " })).toEqual({ name: "이름이 있어야 저장돼요" });
  });

  it.each(["Maru", "ma ru", "-maru", "maru-", "ma--ru", "마루"])("slug 형식 %s", (slug) => {
    expect(draftErrors({ slug })).toEqual({ slug: "영문 소문자·숫자·하이픈만 쓸 수 있어요" });
  });

  it("slug는 소문자·숫자·하이픈이면 통과", () => {
    expect(draftErrors({ slug: "maru-2" })).toEqual({});
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
    const errors = publishErrors({ address: null, summary: null, credits: [] });
    expect(Object.keys(errors)).toEqual(["address", "summary", "credits"]);
  });
});

const SAVED: AdminPlace = {
  ...PUBLISHABLE,
  id: 1,
  visitNote: "주말은 예약",
  status: "draft",
  publishedAt: null,
  mapLinks: { naver: "n", kakao: "k" },
  relatedPlaces: [],
  suggestedBy: null,
  suggestionReason: null,
  suggestionLink: null,
  suggestionMakerHint: null,
  lat: null,
  lng: null,
  createdAt: "2026-10-10T00:00:00Z",
  updatedAt: "2026-10-10T00:00:00Z",
  slugLocked: false,
};

describe("저장 안 된 변경", () => {
  it("불러온 그대로면 변경 아님", () => {
    expect(isDirty(toFormValues(SAVED), SAVED)).toBe(false);
  });

  it("앞뒤 공백·공백뿐인 칸은 변경 아님", () => {
    const values = toFormValues(SAVED);
    expect(isDirty({ ...values, visitNote: "  주말은 예약 ", essay: "   " }, SAVED)).toBe(false);
  });

  it("값을 바꾸거나 비우면 변경", () => {
    const values = toFormValues(SAVED);
    expect(isDirty({ ...values, summary: "다른 한 줄" }, SAVED)).toBe(true);
    expect(isDirty({ ...values, visitNote: "" }, SAVED)).toBe(true);
    expect(isDirty({ ...values, senses: { ...values.senses, touch: "돌" } }, SAVED)).toBe(true);
  });

  it("형식이 틀린 입력은 변경", () => {
    expect(isDirty({ ...toFormValues(SAVED), visitedByEditorOn: "2026-13" }, SAVED)).toBe(true);
  });
});

describe("폼 입력으로 공개 검증", () => {
  it("빈 칸·공백뿐인 칸을 없음으로 본다", () => {
    const values = toFormValues(SAVED);
    expect(formPublishErrors({ ...values, address: "  ", summary: "" }, SAVED)).toEqual({
      address: "주소를 입력해 주세요",
      summary: "한 줄 소개를 입력해 주세요",
    });
  });

  it("하위 목록 규칙은 저장된 목록으로 본다", () => {
    expect(formPublishErrors(toFormValues(SAVED), { ...SAVED, photos: [] })).toEqual({
      photos: "사진 또는 임베드가 1개 이상 필요해요",
    });
  });
});

describe("공개까지 남은 칸", () => {
  const count = (place: Partial<PublishInput>) => publishMissingCount(publishErrors(place));

  it("다 채우면 0칸", () => {
    expect(count({})).toBe(0);
  });

  it("이름만 있는 새 공간은 10칸: 칸 7개 + 사진 또는 임베드 + 출처 있는 크레딧 + 머물러 본 달 또는 출처", () => {
    const empty = {
      slug: null, kind: null, region: null, address: null, access: null, summary: null, impression: null,
      credits: [], sources: [], photos: [], embeds: [], visitedByEditorOn: null,
    };
    expect(count(empty)).toBe(10);
  });

  it("빈 칸 두 개면 2칸", () => {
    expect(count({ slug: null, impression: null })).toBe(2);
  });

  it("사진 줄들의 설명·출처는 몇 줄이 비어도 한 칸", () => {
    const photos = [
      { ...PHOTO, alt: "" },
      { ...PHOTO, credit: "", license: "press" as const, sourceUrl: null },
    ];
    expect(count({ photos })).toBe(1);
  });
});

describe("공개 안내와 라벨 표시", () => {
  it("한 칸 규칙은 짧은 문구, 묶음 규칙은 규칙 문장 그대로", () => {
    expect(hintText("summary", "한 줄 소개를 입력해 주세요")).toBe("공개하려면 필요해요");
    expect(hintText("photos[0].alt", "사진 설명을 입력해 주세요")).toBe("공개하려면 필요해요");
    expect(hintText("credits", "출처가 있는 크레딧이 1개 이상 필요해요")).toBe("출처가 있는 크레딧이 1개 이상 필요해요");
  });

  it("직접 찍은 사진은 출처 링크가 공개에 필요 없다", () => {
    expect([...photoPublishFields({ ...PHOTO, license: "own" })]).toEqual(["alt", "credit"]);
    expect([...photoPublishFields({ ...PHOTO, license: "credited" })]).toEqual(["alt", "credit", "sourceUrl"]);
  });
});

describe("빈 * 칸 표시", () => {
  const fresh: AdminPlace = {
    ...SAVED,
    slug: null, kind: null, region: null, address: null, access: null, visitNote: null, summary: null,
    impression: null, senses: { light: null, sound: null, scent: null, touch: null }, essay: null,
    visitedByEditorOn: null, credits: [], sources: [], photos: [], embeds: [],
    createdAt: "2026-10-10T00:00:00Z", updatedAt: "2026-10-10T00:00:00Z",
  };

  it("새로 만든 공간을 처음 열면 숨긴다", () => {
    expect(marksVisible(fresh, false)).toBe(false);
  });

  it("한 번 저장한 공간(수정 시각이 다름)은 열 때부터 보인다", () => {
    expect(marksVisible({ ...fresh, updatedAt: "2026-10-10T00:05:00Z" }, false)).toBe(true);
  });

  it("이번 화면에서 저장을 눌렀으면 보인다", () => {
    expect(marksVisible(fresh, true)).toBe(true);
  });

  it("수정 시각이 같아도 이름 말고 채운 값이 있으면 보인다(제보로 만든 공간)", () => {
    expect(marksVisible({ ...fresh, region: "경기 양평군" }, false)).toBe(true);
    expect(marksVisible({ ...fresh, senses: { ...fresh.senses, light: "빛" } }, false)).toBe(true);
    expect(marksVisible({ ...fresh, sources: [{ title: "기사", url: "https://e.com", type: "article" }] }, false)).toBe(true);
  });
});

describe("큰 버튼", () => {
  it("작성 중이고 공개 조건이 다 찼을 때만 저장하고 공개", () => {
    expect(saveLabel("draft", 0)).toBe("저장하고 공개");
    expect(saveLabel("draft", 2)).toBe("저장");
    expect(saveLabel("published", 0)).toBe("저장");
    expect(saveLabel("paused", 0)).toBe("저장");
  });
});
