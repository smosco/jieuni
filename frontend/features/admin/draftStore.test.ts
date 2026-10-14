import { describe, expect, it } from "vitest";
import { fromDraft, restorable, toDraft } from "./draftStore";
import { toRows } from "./listSchema";
import { toPhotoRows, type PhotoRow } from "./photoSchema";
import { toFormValues } from "./placeSchema";
import type { AdminPhoto, AdminPlace } from "./types";

const PHOTO: AdminPhoto = {
  id: 1,
  objectKey: "uploads/1/a",
  position: 0,
  url: { w1600: "https://photos/a-1600.webp", w800: "https://photos/a-800.webp" },
  width: 1600,
  height: 1067,
  alt: "마당",
  credit: "지은이",
  license: "own",
  sourceUrl: null,
};

const PLACE = {
  slug: "maru",
  name: "마루",
  kind: "cafe",
  region: "경기 양평군",
  address: null,
  lat: null,
  lng: null,
  access: null,
  visitNote: null,
  summary: null,
  impression: null,
  senses: { light: null, sound: null, scent: null, touch: null },
  essay: null,
  visitedByEditorOn: null,
  credits: [
    {
      position: 0,
      role: "architecture",
      verified: false,
      sourceUrl: "https://example.com/c",
      maker: { id: 3, slug: "studio", name: "스튜디오", kind: "studio" },
      makerId: 3,
    },
  ],
  sources: [],
  embeds: [],
  photos: [PHOTO],
} as unknown as AdminPlace;

const saved = () => toDraft(toFormValues(PLACE), toRows(PLACE), toPhotoRows(PLACE.photos));

const uploaded = (over: Partial<PhotoRow> = {}): PhotoRow => ({
  key: "row-x",
  objectKey: "uploads/1/b",
  width: 1000,
  height: 750,
  preview: "blob:b",
  full: "blob:b2",
  upload: "done",
  uploadError: null,
  file: null,
  alt: "",
  credit: "",
  license: "own",
  sourceUrl: "",
  ...over,
});

describe("저장 안 한 입력 복원 판단", () => {
  it("보관본이 없거나 깨졌으면 묻지 않는다", () => {
    expect(restorable(null, saved())).toBeNull();
    expect(restorable("{", saved())).toBeNull();
    expect(restorable(JSON.stringify({ values: {} }), saved())).toBeNull();
  });

  it("저장된 값과 같으면 묻지 않는다(줄 key·키 순서는 상관없다)", () => {
    const again = toDraft(toFormValues(PLACE), toRows(PLACE), toPhotoRows(PLACE.photos));
    const reordered = { photos: again.photos, lists: again.lists, values: { ...again.values } };
    expect(restorable(JSON.stringify(reordered), saved())).toBeNull();
  });

  it("다르면 보관본을 돌려준다", () => {
    const values = { ...toFormValues(PLACE), summary: "숲 속의 마루" };
    const stored = toDraft(values, toRows(PLACE), toPhotoRows(PLACE.photos));
    expect(restorable(JSON.stringify(stored), saved())?.values.summary).toBe("숲 속의 마루");
  });

  it("올리는 중·실패한 사진은 보관하지 않는다", () => {
    const rows = [
      ...toPhotoRows(PLACE.photos),
      uploaded({ upload: "uploading", objectKey: null }),
      uploaded({ upload: "failed", objectKey: null }),
    ];
    const stored = toDraft(toFormValues(PLACE), toRows(PLACE), rows);
    expect(stored.photos.map((photo) => photo.objectKey)).toEqual(["uploads/1/a"]);
    expect(restorable(JSON.stringify(stored), saved())).toBeNull();
  });

  it("올렸지만 등록 안 한 사진은 보관하고, 불러오면 저장된 사진만 미리보기가 붙는다", () => {
    const stored = toDraft(toFormValues(PLACE), toRows(PLACE), [...toPhotoRows(PLACE.photos), uploaded({ alt: "창" })]);
    const draft = restorable(JSON.stringify(stored), saved());
    expect(draft).not.toBeNull();
    const loaded = fromDraft(draft!, PLACE.photos);
    expect(loaded.photos.map((row) => [row.objectKey, row.preview, row.full, row.alt])).toEqual([
      ["uploads/1/a", "https://photos/a-800.webp", "https://photos/a-1600.webp", "마당"],
      ["uploads/1/b", "", "", "창"],
    ]);
    expect(loaded.photos.every((row) => row.upload === "done" && row.file === null)).toBe(true);
    expect(loaded.lists.credits[0]).toMatchObject({ makerId: 3, sourceUrl: "https://example.com/c" });
  });
});
