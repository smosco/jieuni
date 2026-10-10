import { z } from "zod";
import type { Access, PlaceKind } from "@/shared/api/types";
import type { AdminPlace, PlaceEdit, Senses } from "./types";

const KINDS = [
  "cafe",
  "bar",
  "restaurant",
  "stay",
  "garden",
  "museum",
  "gallery",
  "bookstore",
  "shop",
  "religious",
  "public",
  "other",
] as const satisfies readonly PlaceKind[];

const ACCESSES = ["walk_in", "reservation", "ticket", "guests_only"] as const satisfies readonly Access[];

// 백엔드 slug 검사·DB CHECK와 같은 식
const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const YEAR_MONTH = /^\d{4}-(0[1-9]|1[0-2])$/;

// 입력칸 값. 선택 상자·숫자 칸도 입력 그대로 문자열로 들고 있다가 저장할 때 바꾼다.
export type PlaceFormValues = Omit<
  { [K in keyof PlaceEdit]: string },
  "senses"
> & { senses: Senses<string> };

// 서버도 앞뒤 공백을 지우고 빈 칸을 null로 저장한다. 길이는 지운 뒤에 잰다.
function text(max: number, message: string) {
  return z
    .string()
    .trim()
    .max(max, message)
    .transform((value) => value || null);
}

function choice<const T extends readonly [string, ...string[]]>(values: T) {
  return z.union([z.literal(""), z.enum(values)]).transform((value) => value || null);
}

function coordinate(limit: number, rangeMessage: string) {
  return z
    .string()
    .trim()
    .transform((value, ctx) => {
      if (value === "") return null;
      const number = Number(value);
      // 서버는 숫자가 아닌 값을 칸 이름 없는 400으로 돌려주므로 여기서 막는다.
      if (!Number.isFinite(number)) {
        ctx.addIssue("숫자로 입력해 주세요");
        return z.NEVER;
      }
      if (Math.abs(number) > limit) {
        ctx.addIssue(rangeMessage);
        return z.NEVER;
      }
      return number;
    });
}

const sense = text(60, "60자까지 쓸 수 있어요");

// 임시저장: 형식만. 백엔드 PATCH 요청의 검증과 같은 길이·문구.
export const draftSchema = z.object({
  slug: text(80, "주소(slug)는 80자까지 쓸 수 있어요").refine(
    (value) => value === null || SLUG.test(value),
    "영문 소문자·숫자·하이픈만 쓸 수 있어요",
  ),
  name: z.string().trim().min(1, "공간 이름을 입력해 주세요").max(100, "공간 이름은 100자까지 쓸 수 있어요"),
  kind: choice(KINDS),
  region: text(50, "지역은 50자까지 쓸 수 있어요"),
  address: text(200, "주소는 200자까지 쓸 수 있어요"),
  lat: coordinate(90, "위도는 -90에서 90 사이예요"),
  lng: coordinate(180, "경도는 -180에서 180 사이예요"),
  access: choice(ACCESSES),
  visitNote: text(100, "100자까지 쓸 수 있어요"),
  summary: text(60, "60자까지 쓸 수 있어요"),
  impression: text(400, "400자까지 쓸 수 있어요"),
  senses: z.object({ light: sense, sound: sense, scent: sense, touch: sense }),
  essay: z
    .string()
    .trim()
    .transform((value) => value || null),
  // 서버는 형식이 틀린 년-월을 칸 이름 없는 400으로 돌려주므로 여기서 막는다.
  visitedByEditorOn: z
    .string()
    .trim()
    .refine((value) => value === "" || YEAR_MONTH.test(value), "2026-05처럼 년-월로 입력해 주세요")
    .transform((value) => value || null),
}) satisfies z.ZodType<PlaceEdit, PlaceFormValues>;

export type PublishInput = Pick<
  AdminPlace,
  | keyof PlaceEdit
  | "credits"
  | "sources"
  | "photos"
  | "embeds"
>;

function blank(value: unknown): boolean {
  return value === null || value === undefined || (typeof value === "string" && value.trim() === "");
}

// 공개 시 검증: API.md의 PublishRules 표를 키·조건·문구·순서 그대로 옮겼다.
export const publishSchema = z.custom<PublishInput>().superRefine((place, ctx) => {
  const fail = (path: (string | number)[], message: string) => ctx.addIssue({ code: "custom", path, message });
  const need = (key: keyof PlaceEdit, message: string) => {
    if (blank(place[key])) fail([key], message);
  };

  need("slug", "주소(slug)를 정해 주세요");
  need("name", "공간 이름을 입력해 주세요");
  need("kind", "종류를 골라 주세요");
  need("region", "지역을 입력해 주세요");
  need("address", "주소를 입력해 주세요");
  need("lat", "좌표를 입력해 주세요");
  need("lng", "좌표를 입력해 주세요");
  need("access", "방문 방식을 골라 주세요");
  need("summary", "한 줄 소개를 입력해 주세요");
  need("impression", "감상을 입력해 주세요");

  if (!place.credits.some((credit) => !blank(credit.sourceUrl))) {
    fail(["credits"], "출처가 있는 크레딧이 1개 이상 필요해요");
  }
  if (blank(place.visitedByEditorOn) && place.sources.length === 0) {
    fail(["sources"], "머물러 본 날이 없으면 출처가 1개 이상 필요해요");
  }
  if (place.photos.length === 0 && place.embeds.length === 0) {
    fail(["photos"], "사진 또는 임베드가 1개 이상 필요해요");
  }
  place.photos.forEach((photo, i) => {
    if (blank(photo.alt)) fail(["photos", i, "alt"], "사진 설명을 입력해 주세요");
    if (blank(photo.credit)) fail(["photos", i, "credit"], "사진 출처(이름)를 입력해 주세요");
    if (photo.license !== "own" && blank(photo.sourceUrl)) fail(["photos", i, "sourceUrl"], "출처 링크를 입력해 주세요");
  });
});

// Zod 경로를 서버 fieldErrors 키 모양으로: ["senses","touch"] → "senses.touch", ["photos",1,"alt"] → "photos[1].alt".
// 칸마다 첫 메시지만 둔다(서버와 같다).
export function toFieldErrors(issues: readonly z.core.$ZodIssue[]): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const issue of issues) {
    const key = issue.path
      .map((part, i) => (typeof part === "number" ? `[${part}]` : i === 0 ? String(part) : `.${String(part)}`))
      .join("");
    errors[key] ??= issue.message;
  }
  return errors;
}

// 서버 값 → 입력칸 값. null은 빈 칸.
export function toFormValues(place: AdminPlace): PlaceFormValues {
  const str = (value: string | number | null) => (value === null ? "" : String(value));
  return {
    slug: str(place.slug),
    name: place.name,
    kind: str(place.kind),
    region: str(place.region),
    address: str(place.address),
    lat: str(place.lat),
    lng: str(place.lng),
    access: str(place.access),
    visitNote: str(place.visitNote),
    summary: str(place.summary),
    impression: str(place.impression),
    senses: {
      light: str(place.senses.light),
      sound: str(place.senses.sound),
      scent: str(place.senses.scent),
      touch: str(place.senses.touch),
    },
    essay: str(place.essay),
    visitedByEditorOn: str(place.visitedByEditorOn),
  };
}

// 서버 값 → PATCH 본문 모양(키 순서도 draftSchema 출력과 같다).
export function toEdit(place: AdminPlace): PlaceEdit {
  return {
    slug: place.slug,
    name: place.name,
    kind: place.kind,
    region: place.region,
    address: place.address,
    lat: place.lat,
    lng: place.lng,
    access: place.access,
    visitNote: place.visitNote,
    summary: place.summary,
    impression: place.impression,
    senses: {
      light: place.senses.light,
      sound: place.senses.sound,
      scent: place.senses.scent,
      touch: place.senses.touch,
    },
    essay: place.essay,
    visitedByEditorOn: place.visitedByEditorOn,
  };
}

// 저장 안 된 변경: 입력을 저장할 때처럼 정규화해 저장된 값과 비교한다. 앞뒤 공백·빈 칸 차이는 변경이 아니다.
// 형식이 틀린 입력은 저장된 값일 수 없으니 변경이다.
export function isDirty(values: PlaceFormValues, place: AdminPlace): boolean {
  const parsed = draftSchema.safeParse(values);
  return !parsed.success || JSON.stringify(parsed.data) !== JSON.stringify(toEdit(place));
}

// 지금 입력 + 저장된 하위 목록으로 공개 검증. "비어 있음"은 정규화 전 문자열로 봐도 같다.
export function publishErrors(values: PlaceFormValues, place: AdminPlace): Record<string, string> {
  const result = publishSchema.safeParse({ ...place, ...values });
  return result.success ? {} : toFieldErrors(result.error.issues);
}
