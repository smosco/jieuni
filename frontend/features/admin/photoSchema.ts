import { z } from "zod";
import type { PhotoLicense } from "@/shared/api/types";
import { optionalLink, picked, rowKey } from "./listSchema";
import { toFieldErrors } from "./placeSchema";
import type { AdminPhoto, PhotoBody, PhotoEdit } from "./types";

const LICENSES = ["own", "press", "credited"] as const satisfies readonly PhotoLicense[];

// 파일은 고르자마자 바꿔서 저장소에 올리고(converting → uploading → done), 등록은 임시저장 때 한다.
export type UploadState = "converting" | "uploading" | "failed" | "done";

// 입력 중인 사진 한 장. 저장된 사진과는 objectKey로 짝짓는다(새 사진은 저장 뒤에 id를 받는다).
export type PhotoRow = {
  key: string;
  objectKey: string | null;
  width: number;
  height: number;
  preview: string;
  upload: UploadState;
  uploadError: string | null;
  file: File | null;
  alt: string;
  credit: string;
  license: PhotoLicense | "";
  sourceUrl: string;
};

// 백엔드 사진 API의 검증과 같은 길이·문구.
const photoRow = z
  .object({
    alt: z.string().trim().max(200, "200자까지 쓸 수 있어요").transform((value) => value || null),
    credit: z.string().trim().max(100, "100자까지 쓸 수 있어요").transform((value) => value || null),
    license: picked(LICENSES, "사진 종류를 골라 주세요"),
    sourceUrl: optionalLink,
  })
  .superRefine((row, ctx) => {
    if (row.license !== "own" && row.sourceUrl === null) {
      ctx.addIssue({ code: "custom", path: ["sourceUrl"], message: "출처 링크를 입력해 주세요" });
    }
  })
  .transform((row): PhotoEdit => ({ alt: row.alt, credit: row.credit, license: row.license, sourceUrl: row.sourceUrl }));

// 목록 이름으로 감싸 오류 키가 서버 공개 검증과 같게("photos[0].alt") 나온다.
const photosSchema = z.object({ photos: z.array(photoRow) });

export function toPhotoRows(photos: AdminPhoto[]): PhotoRow[] {
  return photos.map((photo) => ({
    key: rowKey(),
    objectKey: photo.objectKey,
    width: photo.width,
    height: photo.height,
    preview: photo.url.w800,
    upload: "done",
    uploadError: null,
    file: null,
    alt: photo.alt ?? "",
    credit: photo.credit ?? "",
    license: photo.license,
    sourceUrl: photo.sourceUrl ?? "",
  }));
}

function body(photo: AdminPhoto): PhotoBody {
  return {
    objectKey: photo.objectKey,
    width: photo.width,
    height: photo.height,
    alt: photo.alt,
    credit: photo.credit,
    license: photo.license,
    sourceUrl: photo.sourceUrl,
  };
}

export type PhotoCheck = {
  // 바꾸거나 올리는 중인 사진이 있다. 저장을 막는다.
  busy: boolean;
  // 올리지 못한 사진이 있다. 다시 올리거나 빼야 저장할 수 있다.
  failed: boolean;
  dirty: boolean;
  errors: Record<string, string>;
  // 형식이 맞고 저장된 것과 다를 때만: 저장 뒤 사진 목록이 이 순서·값이 된다.
  changes?: PhotoBody[];
};

// 입력을 저장할 때처럼 정규화해 저장된 사진과 비교한다. 올리는 중이거나 형식이 틀리면 저장된 상태일 수 없으니 변경이다.
export function checkPhotos(rows: PhotoRow[], saved: AdminPhoto[]): PhotoCheck {
  const busy = rows.some((row) => row.upload === "converting" || row.upload === "uploading");
  const failed = rows.some((row) => row.upload === "failed");
  const result = photosSchema.safeParse({ photos: rows });
  if (!result.success) {
    return { busy, failed, dirty: true, errors: toFieldErrors(result.error.issues) };
  }
  if (busy || failed) return { busy, failed, dirty: true, errors: {} };
  const desired = rows.map((row, i): PhotoBody => ({
    objectKey: row.objectKey as string,
    width: row.width,
    height: row.height,
    ...result.data.photos[i],
  }));
  const dirty = JSON.stringify(desired) !== JSON.stringify(saved.map(body));
  return { busy, failed, dirty, errors: {}, changes: dirty ? desired : undefined };
}

export type PhotoSteps = {
  // index는 desired 안의 자리다. 서버 오류를 그 줄("photos[i].…")에 보이려고 쓴다.
  add: { index: number; body: PhotoBody }[];
  edit: { index: number; id: number; body: PhotoEdit }[];
  remove: number[];
};

// 저장된 사진과 원하는 목록의 차이. 순서는 등록·삭제가 끝난 뒤 orderIds로 따로 맞춘다.
export function photoSteps(desired: PhotoBody[], saved: AdminPhoto[]): PhotoSteps {
  const byKey = new Map(saved.map((photo) => [photo.objectKey, photo]));
  const keep = new Set(desired.map((photo) => photo.objectKey));
  const steps: PhotoSteps = { add: [], edit: [], remove: [] };
  desired.forEach((photo, index) => {
    const old = byKey.get(photo.objectKey);
    const edit: PhotoEdit = { alt: photo.alt, credit: photo.credit, license: photo.license, sourceUrl: photo.sourceUrl };
    if (!old) {
      steps.add.push({ index, body: photo });
    } else if (JSON.stringify(edit) !== JSON.stringify({ alt: old.alt, credit: old.credit, license: old.license, sourceUrl: old.sourceUrl })) {
      steps.edit.push({ index, id: old.id, body: edit });
    }
  });
  steps.remove = saved.filter((photo) => !keep.has(photo.objectKey)).map((photo) => photo.id);
  return steps;
}

// 서버 사진(position 순)이 원하는 순서와 다르면 순서 교체 본문, 같으면 null.
export function orderIds(desired: PhotoBody[], current: AdminPhoto[]): number[] | null {
  const wanted = desired.map((photo) => photo.objectKey);
  if (JSON.stringify(wanted) === JSON.stringify(current.map((photo) => photo.objectKey))) return null;
  const ids = new Map(current.map((photo) => [photo.objectKey, photo.id]));
  return wanted.map((key) => ids.get(key) as number);
}

// 서버 공개 검증 키 "photos[N].x"의 N은 서버의 사진 순서(serverKeys)다. 순서 교체는 저장 끝에 보내므로
// 그 전의 오류는 폼 줄 순서(desired)와 다를 수 있어 같은 사진의 줄 번호로 바꾼다. 이번 저장에서 빠질 사진이면 목록 아래에 단다.
export function toFormPhotoKey(key: string, serverKeys: string[], desired: PhotoBody[]): string {
  const match = /^photos\[(\d+)\](.*)$/.exec(key);
  if (!match) return key;
  const index = desired.findIndex((photo) => photo.objectKey === serverKeys[Number(match[1])]);
  return index < 0 ? "photos" : `photos[${index}]${match[2]}`;
}

// 너비를 max 이하로 줄인다. 작은 사진은 키우지 않는다. 공개 화면이 너비 기준 srcset(800w·1600w)을 쓴다.
export function fitWidth(width: number, height: number, max: number): { width: number; height: number } {
  if (width <= max) return { width, height };
  return { width: max, height: Math.max(1, Math.round((height * max) / width)) };
}
