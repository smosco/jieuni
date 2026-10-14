import { z } from "./zod";
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

const sense = text(60, "60자까지 쓸 수 있어요");

// 저장: 이름만 있으면 된다. 나머지는 형식만 본다(백엔드 PATCH 요청의 검증과 같은 길이·문구).
export const draftSchema = z.object({
  slug: text(80, "주소(slug)는 80자까지 쓸 수 있어요").refine(
    (value) => value === null || SLUG.test(value),
    "영문 소문자·숫자·하이픈만 쓸 수 있어요",
  ),
  name: z.string().trim().min(1, "이름이 있어야 저장돼요").max(100, "공간 이름은 100자까지 쓸 수 있어요"),
  kind: choice(KINDS),
  region: text(50, "지역은 50자까지 쓸 수 있어요"),
  address: text(200, "주소는 200자까지 쓸 수 있어요"),
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

type PublishPhoto = { alt: string | null; credit: string | null; license: string; sourceUrl: string | null };

// 공개 시 검증: API.md의 PublishRules 표를 키·조건·문구·순서 그대로 옮겼다. 검증·남은 칸 수·라벨 * 이 모두 이 표를 본다.
// item은 "공개까지 n칸"에서 한 칸으로 세는 단위다.
const FIELD_RULES: { key: keyof PlaceEdit; item: string; message: string }[] = [
  { key: "slug", item: "주소(slug)", message: "주소(slug)를 정해 주세요" },
  { key: "name", item: "공간 이름", message: "공간 이름을 입력해 주세요" },
  { key: "kind", item: "종류", message: "종류를 골라 주세요" },
  { key: "region", item: "지역", message: "지역을 입력해 주세요" },
  { key: "address", item: "주소", message: "주소를 입력해 주세요" },
  { key: "access", item: "방문 방식", message: "방문 방식을 골라 주세요" },
  { key: "summary", item: "한 줄 소개", message: "한 줄 소개를 입력해 주세요" },
  { key: "impression", item: "감상", message: "감상을 입력해 주세요" },
];

const GROUP_RULES: { key: "credits" | "sources" | "photos"; item: string; message: string; missing: (place: PublishInput) => boolean }[] = [
  {
    key: "credits",
    item: "출처가 있는 크레딧",
    message: "출처가 있는 크레딧이 1개 이상 필요해요",
    missing: (place) => !place.credits.some((credit) => !blank(credit.sourceUrl)),
  },
  {
    key: "sources",
    item: "머물러 본 달 또는 출처",
    message: "머물러 본 날이 없으면 출처가 1개 이상 필요해요",
    missing: (place) => blank(place.visitedByEditorOn) && place.sources.length === 0,
  },
  {
    key: "photos",
    item: "사진 또는 임베드",
    message: "사진 또는 임베드가 1개 이상 필요해요",
    missing: (place) => place.photos.length === 0 && place.embeds.length === 0,
  },
];

const PHOTO_ITEM = "사진 설명·출처";
const PHOTO_RULES: { field: keyof PublishPhoto; message: string; applies: (photo: PublishPhoto) => boolean }[] = [
  { field: "alt", message: "사진 설명을 입력해 주세요", applies: () => true },
  { field: "credit", message: "사진 출처(이름)를 입력해 주세요", applies: () => true },
  { field: "sourceUrl", message: "출처 링크를 입력해 주세요", applies: (photo) => photo.license !== "own" },
];

export const publishSchema = z.custom<PublishInput>().superRefine((place, ctx) => {
  const fail = (path: (string | number)[], message: string) => ctx.addIssue({ code: "custom", path, message });
  for (const rule of FIELD_RULES) {
    if (blank(place[rule.key])) fail([rule.key], rule.message);
  }
  for (const rule of GROUP_RULES) {
    if (rule.missing(place)) fail([rule.key], rule.message);
  }
  place.photos.forEach((photo, i) => {
    for (const rule of PHOTO_RULES) {
      if (rule.applies(photo) && blank(photo[rule.field])) fail(["photos", i, rule.field], rule.message);
    }
  });
});

// 라벨에 * 을 붙일 칸.
export const PUBLISH_FIELDS: ReadonlySet<string> = new Set(FIELD_RULES.map((rule) => rule.key));

export function photoPublishFields(photo: PublishPhoto): ReadonlySet<string> {
  return new Set(PHOTO_RULES.filter((rule) => rule.applies(photo)).map((rule) => rule.field));
}

const GROUP_KEYS: ReadonlySet<string> = new Set(GROUP_RULES.map((rule) => rule.key));

// 칸 아래에 보일 공개 안내. 한 칸 규칙은 짧게, 묶음 규칙은 무엇이 필요한지 문장 그대로.
export function hintText(key: string, message: string): string {
  return GROUP_KEYS.has(key) ? message : "공개하려면 필요해요";
}

// 공개까지 남은 칸 수. 같은 item은 한 칸, 사진 줄들의 설명·출처는 모두 합쳐 한 칸으로 센다.
export function publishMissingCount(errors: Record<string, string>): number {
  const missing = new Set<string>();
  for (const rule of [...FIELD_RULES, ...GROUP_RULES]) {
    if (errors[rule.key] !== undefined) missing.add(rule.item);
  }
  if (Object.keys(errors).some((key) => key.startsWith("photos["))) missing.add(PHOTO_ITEM);
  return missing.size;
}

// 빈 * 칸 표시를 켤지. 새로 만든 공간을 처음 열 때만 숨긴다: 한 번 저장했거나(수정 시각이 다름),
// 이름 말고 채운 값이 있거나(제보로 만든 공간), 이번 화면에서 저장을 눌렀으면 보인다.
export function marksVisible(place: AdminPlace, submitted: boolean): boolean {
  if (submitted || place.updatedAt !== place.createdAt) return true;
  const filled = Object.entries(toEdit(place)).some(([key, value]) =>
    key === "name" ? false : key === "senses" ? Object.values(value as object).some((v) => !blank(v)) : !blank(value),
  );
  return filled || place.credits.length + place.sources.length + place.embeds.length + place.photos.length > 0;
}

// 아래 줄의 큰 버튼. 작성 중이고 공개 조건이 다 찼을 때만 저장과 공개를 한 번에 한다.
export function saveLabel(status: AdminPlace["status"], missing: number): "저장" | "저장하고 공개" {
  return status === "draft" && missing === 0 ? "저장하고 공개" : "저장";
}

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
  const str = (value: string | null) => (value === null ? "" : String(value));
  return {
    slug: str(place.slug),
    name: place.name,
    kind: str(place.kind),
    region: str(place.region),
    address: str(place.address),
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

// 공개 검증에 필요한 목록 모양. 입력 중인 줄(문자열 칸)도 그대로 넣을 수 있다.
type PublishLists = {
  credits: { sourceUrl: string | null }[];
  sources: unknown[];
  embeds: unknown[];
  photos?: { alt: string | null; credit: string | null; license: string; sourceUrl: string | null }[];
};

// 지금 입력으로 공개 검증. 목록을 넘기지 않으면 저장된 목록을 쓴다.
// "비어 있음"은 정규화 전 문자열로 봐도 같다.
export function publishErrors(values: PlaceFormValues, place: AdminPlace, lists?: PublishLists): Record<string, string> {
  const result = publishSchema.safeParse({ ...place, ...values, ...lists });
  return result.success ? {} : toFieldErrors(result.error.issues);
}
