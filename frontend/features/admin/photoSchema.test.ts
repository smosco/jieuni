import { describe, expect, it } from "vitest";
import { rowKey } from "./listSchema";
import { checkPhotos, fitWidth, orderIds, photoSteps, toFormPhotoKey, toPhotoRows, type PhotoRow } from "./photoSchema";
import type { AdminPhoto, PhotoBody } from "./types";

const saved = (id: number, objectKey: string, over: Partial<AdminPhoto> = {}): AdminPhoto => ({
  id,
  objectKey,
  position: id,
  url: { w1600: `https://photos.test/${objectKey}-1600.webp`, w800: `https://photos.test/${objectKey}-800.webp` },
  width: 1600,
  height: 1067,
  alt: "정원",
  credit: "에디터",
  license: "own",
  sourceUrl: null,
  ...over,
});

const SAVED = [saved(1, "uploads/1/a"), saved(2, "uploads/1/b", { license: "press", sourceUrl: "https://example.com/p" })];

const fresh = (over: Partial<PhotoRow> = {}): PhotoRow => ({
  key: rowKey(),
  objectKey: "uploads/1/c",
  width: 800,
  height: 1200,
  preview: "blob:x",
  full: "blob:y",
  upload: "done",
  uploadError: null,
  file: null,
  alt: "창",
  credit: "에디터",
  license: "own",
  sourceUrl: "",
  ...over,
});

const body = (objectKey: string, over: Partial<PhotoBody> = {}): PhotoBody => ({
  objectKey,
  width: 1600,
  height: 1067,
  alt: "정원",
  credit: "에디터",
  license: "own",
  sourceUrl: null,
  ...over,
});

describe("사진 변경 판단", () => {
  it("불러온 그대로면 변경 없음", () => {
    expect(checkPhotos(toPhotoRows(SAVED), SAVED)).toEqual({ busy: false, failed: false, dirty: false, errors: {} });
  });

  it("앞뒤 공백·빈 칸은 저장할 때처럼 정규화해 비교한다", () => {
    const rows = toPhotoRows(SAVED);
    rows[0].alt = " 정원 ";
    rows[0].sourceUrl = "  ";
    expect(checkPhotos(rows, SAVED).dirty).toBe(false);
  });

  it("새로 올린 사진은 변경이고 원하는 목록에 들어간다", () => {
    const result = checkPhotos([...toPhotoRows(SAVED), fresh({ alt: "", credit: "" })], SAVED);
    expect(result.dirty).toBe(true);
    expect(result.changes?.[2]).toEqual({
      objectKey: "uploads/1/c",
      width: 800,
      height: 1200,
      alt: null,
      credit: null,
      license: "own",
      sourceUrl: null,
    });
  });

  it("올리는 중이거나 실패한 사진이 있으면 저장할 목록을 만들지 않는다", () => {
    const busy = checkPhotos([fresh({ upload: "uploading", objectKey: null })], []);
    expect(busy).toMatchObject({ busy: true, failed: false, dirty: true });
    expect(busy.changes).toBeUndefined();
    expect(checkPhotos([fresh({ upload: "failed", objectKey: null })], [])).toMatchObject({ failed: true, dirty: true });
  });
});

describe("사진 검증", () => {
  it("직접 찍은 사진이 아니면 출처 링크가 필요하다", () => {
    const rows = [fresh({ license: "credited" }), fresh({ license: "own" })];
    expect(checkPhotos(rows, []).errors).toEqual({ "photos[0].sourceUrl": "출처 링크를 입력해 주세요" });
  });

  it("종류를 고르지 않으면 막고, 길이·링크 형식은 서버와 같은 문구", () => {
    const rows = [fresh({ license: "", alt: "가".repeat(201), credit: "나".repeat(101), sourceUrl: "ftp://x" })];
    expect(checkPhotos(rows, []).errors).toEqual({
      "photos[0].alt": "200자까지 쓸 수 있어요",
      "photos[0].credit": "100자까지 쓸 수 있어요",
      "photos[0].license": "사진 종류를 골라 주세요",
      "photos[0].sourceUrl": "https://로 시작하는 주소를 넣어 주세요",
    });
  });
});

describe("사진 저장 단계", () => {
  it("새 키는 등록, 값이 바뀐 것은 수정, 빠진 것은 삭제", () => {
    const desired = [body("uploads/1/c"), body("uploads/1/a", { alt: "빛" })];
    expect(photoSteps(desired, SAVED)).toEqual({
      add: [{ index: 0, body: desired[0] }],
      edit: [{ index: 1, id: 1, body: { alt: "빛", credit: "에디터", license: "own", sourceUrl: null } }],
      remove: [2],
    });
  });

  it("값이 같으면 아무것도 보내지 않는다", () => {
    const desired = SAVED.map((photo) => body(photo.objectKey, { license: photo.license, sourceUrl: photo.sourceUrl }));
    expect(photoSteps(desired, SAVED)).toEqual({ add: [], edit: [], remove: [] });
  });

  it("순서가 다를 때만 id 순서를 준다", () => {
    expect(orderIds([body("uploads/1/a"), body("uploads/1/b")], SAVED)).toBeNull();
    expect(orderIds([body("uploads/1/b"), body("uploads/1/a")], SAVED)).toEqual([2, 1]);
  });
});

describe("서버 오류 줄 번호", () => {
  it("서버 순서의 자리를 폼 줄 번호로 바꾼다", () => {
    // 저장된 [a, b]에서 b를 맨 위로 올리고 c를 새로 더한 폼. 서버는 아직 [a, b, c] 순서다.
    const desired = [body("uploads/1/b"), body("uploads/1/a"), body("uploads/1/c")];
    const server = ["uploads/1/a", "uploads/1/b", "uploads/1/c"];
    expect(toFormPhotoKey("photos[1].credit", server, desired)).toBe("photos[0].credit");
    expect(toFormPhotoKey("photos[0].alt", server, desired)).toBe("photos[1].alt");
    expect(toFormPhotoKey("photos[2].sourceUrl", server, desired)).toBe("photos[2].sourceUrl");
    // 이번 저장에서 빠질 사진의 오류는 목록 아래로, 목록 오류는 그대로.
    expect(toFormPhotoKey("photos[1].alt", server, [body("uploads/1/a")])).toBe("photos");
    expect(toFormPhotoKey("photos", server, desired)).toBe("photos");
  });
});

describe("너비 맞추기", () => {
  it("너비를 줄이고 비율을 지킨다", () => {
    expect(fitWidth(4032, 3024, 1600)).toEqual({ width: 1600, height: 1200 });
    expect(fitWidth(3024, 4032, 800)).toEqual({ width: 800, height: 1067 });
  });

  it("작은 사진은 키우지 않는다", () => {
    expect(fitWidth(1200, 900, 1600)).toEqual({ width: 1200, height: 900 });
  });
});
