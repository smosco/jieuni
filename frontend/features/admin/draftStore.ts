import { rowKey, type ListRows } from "./listSchema";
import type { PhotoRow } from "./photoSchema";
import type { PlaceFormValues } from "./placeSchema";
import type { AdminPhoto } from "./types";

// 저장하지 않은 입력을 공간마다 브라우저에 보관한다. 새로고침·뒤로 가기로 폼을 떠나도 다시 열면 불러올 수 있게.
const PREFIX = "jieuni:admin-place:";

type Unkeyed<T> = Omit<T, "key">;
type StoredPhoto = Pick<PhotoRow, "objectKey" | "width" | "height" | "alt" | "credit" | "license" | "sourceUrl">;

export type PlaceDraft = {
  values: PlaceFormValues;
  lists: { [K in keyof ListRows]: Unkeyed<ListRows[K][number]>[] };
  photos: StoredPhoto[];
};

function unkeyed<T extends { key: string }>(rows: T[]): Unkeyed<T>[] {
  return rows.map((row) => {
    const copy: Partial<T> = { ...row };
    delete copy.key;
    return copy as Unkeyed<T>;
  });
}

// 줄 key는 화면용이라 빼고, 사진은 저장소에 올라간 것만 남긴다(파일·blob은 새로고침하면 사라진다).
export function toDraft(values: PlaceFormValues, lists: ListRows, photos: PhotoRow[]): PlaceDraft {
  return {
    values,
    lists: { credits: unkeyed(lists.credits), sources: unkeyed(lists.sources), embeds: unkeyed(lists.embeds) },
    photos: photos
      .filter((row) => row.upload === "done" && row.objectKey !== null)
      .map(({ objectKey, width, height, alt, credit, license, sourceUrl }) => ({
        objectKey,
        width,
        height,
        alt,
        credit,
        license,
        sourceUrl,
      })),
  };
}

// 보관본을 폼 상태로. 저장된 사진은 미리보기를 다시 붙이고, 아직 등록 안 된 사진은 미리보기 없이 둔다.
export function fromDraft(draft: PlaceDraft, saved: AdminPhoto[]): { values: PlaceFormValues; lists: ListRows; photos: PhotoRow[] } {
  const urls = new Map(saved.map((photo) => [photo.objectKey, photo.url]));
  return {
    values: draft.values,
    lists: {
      credits: draft.lists.credits.map((row) => ({ ...row, key: rowKey() })),
      sources: draft.lists.sources.map((row) => ({ ...row, key: rowKey() })),
      embeds: draft.lists.embeds.map((row) => ({ ...row, key: rowKey() })),
    },
    photos: draft.photos.map((photo) => ({
      ...photo,
      key: rowKey(),
      preview: urls.get(photo.objectKey as string)?.w800 ?? "",
      full: urls.get(photo.objectKey as string)?.w1600 ?? "",
      upload: "done",
      uploadError: null,
      file: null,
    })),
  };
}

// 키 순서와 상관없이 같은 값이면 같은 문자열.
function stable(value: unknown): string {
  if (Array.isArray(value)) return `[${value.map(stable).join(",")}]`;
  if (value && typeof value === "object") {
    const entries = Object.entries(value).sort(([a], [b]) => a.localeCompare(b));
    return `{${entries.map(([key, v]) => `${JSON.stringify(key)}:${stable(v)}`).join(",")}}`;
  }
  return JSON.stringify(value);
}

function isDraft(value: unknown): value is PlaceDraft {
  const draft = value as PlaceDraft;
  return (
    typeof draft?.values === "object" &&
    draft.values !== null &&
    Array.isArray(draft.lists?.credits) &&
    Array.isArray(draft.lists?.sources) &&
    Array.isArray(draft.lists?.embeds) &&
    Array.isArray(draft.photos)
  );
}

// 다시 열었을 때 물어볼 보관본. 없거나 깨졌거나 지금 저장된 값과 같으면 null.
export function restorable(stored: string | null, current: PlaceDraft): PlaceDraft | null {
  if (!stored) return null;
  let parsed: unknown;
  try {
    parsed = JSON.parse(stored);
  } catch {
    return null;
  }
  if (!isDraft(parsed) || stable(parsed) === stable(current)) return null;
  return parsed;
}

// 사생활 보호 모드·저장소 차단에서는 읽기·쓰기가 던질 수 있다. 보관은 편의라 실패해도 폼은 그대로 쓴다.
export function readDraft(id: number): string | null {
  try {
    return window.localStorage.getItem(PREFIX + id);
  } catch {
    return null;
  }
}

export function writeDraft(id: number, draft: PlaceDraft): void {
  try {
    window.localStorage.setItem(PREFIX + id, JSON.stringify(draft));
  } catch {
    // 보관하지 못해도 이탈 확인은 남아 있다.
  }
}

export function clearDraft(id: number): void {
  try {
    window.localStorage.removeItem(PREFIX + id);
  } catch {
    // 지우지 못한 보관본은 다음에 열 때 저장된 값과 같으면 묻지 않는다.
  }
}
