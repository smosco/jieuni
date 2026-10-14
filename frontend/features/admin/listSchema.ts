import { z } from "./zod";
import type { SourceType } from "@/features/place/types";
import type { CreditRole } from "@/shared/api/types";
import { embedSource, embedTypeOf } from "@/shared/lib/embed";
import { toFieldErrors } from "./placeSchema";
import type { AdminPlace, PlaceLists } from "./types";

const ROLES = [
  "planning",
  "branding",
  "architecture",
  "interior",
  "landscape",
  "lighting",
  "furniture",
  "scent",
  "photography",
  "owner",
  "other",
] as const satisfies readonly CreditRole[];

const SOURCE_TYPES = ["article", "video", "instagram", "official"] as const satisfies readonly SourceType[];

// 백엔드와 같은 식: 공개 화면에서 눌리는 링크라 https만.
const HTTPS_URL = /^https:\/\/\S+$/;
export const HTTPS_URL_MESSAGE = "https://로 시작하는 주소를 넣어 주세요";

// 입력 중인 한 줄. key는 순서를 바꿔도 React가 같은 줄로 알아보게 붙이는 화면용 값이다.
export type CreditRow = {
  key: string;
  makerId: number | null;
  makerName: string;
  role: CreditRole | "";
  verified: boolean;
  sourceUrl: string;
};
export type SourceRow = { key: string; title: string; url: string; type: SourceType | "" };
export type EmbedRow = { key: string; url: string };
export type ListRows = { credits: CreditRow[]; sources: SourceRow[]; embeds: EmbedRow[] };
export type ListName = keyof ListRows;

let next = 0;
export function rowKey(): string {
  next += 1;
  return `row-${next}`;
}

export const optionalLink = z
  .string()
  .trim()
  .max(500, "링크는 500자까지 쓸 수 있어요")
  .refine((value) => value === "" || HTTPS_URL.test(value), HTTPS_URL_MESSAGE)
  .transform((value) => value || null);

const requiredLink = z
  .string()
  .trim()
  .min(1, "링크를 입력해 주세요")
  .max(500, "링크는 500자까지 쓸 수 있어요")
  .refine((value) => HTTPS_URL.test(value), HTTPS_URL_MESSAGE);

export function picked<const T extends readonly [string, ...string[]]>(values: T, message: string) {
  return z
    .union([z.literal(""), z.enum(values)])
    .refine((value) => value !== "", message)
    .transform((value) => value as T[number]);
}

// 백엔드 통째 교체 API의 검증과 같은 문구. 확인됨 ⇒ 출처는 서버에서는 서비스가 보는 규칙이다.
const creditRow = z
  .object({
    makerId: z
      .number()
      .nullable()
      .refine((value) => value !== null, "만든 사람을 골라 주세요"),
    role: picked(ROLES, "역할을 골라 주세요"),
    verified: z.boolean(),
    sourceUrl: optionalLink,
  })
  .superRefine((row, ctx) => {
    if (row.verified && row.sourceUrl === null) {
      ctx.addIssue({ code: "custom", path: ["sourceUrl"], message: "확인됨이면 출처 링크가 필요해요" });
    }
  })
  .transform((row) => ({ makerId: row.makerId as number, role: row.role, verified: row.verified, sourceUrl: row.sourceUrl }));

const sourceRow = z
  .object({
    title: z.string().trim().min(1, "제목을 입력해 주세요").max(200, "제목은 200자까지 쓸 수 있어요"),
    url: requiredLink,
    type: picked(SOURCE_TYPES, "종류를 골라 주세요"),
  })
  .transform((row) => ({ title: row.title, url: row.url, type: row.type }));

// 종류는 고르지 않고 주소로 정한다. 공개 화면이 그릴 수 있는 주소(영상·게시물)만 받는다.
const embedRow = z.object({ url: requiredLink }).transform((row, ctx) => {
  const type = embedTypeOf(row.url);
  if (!type || !embedSource({ type, url: row.url })) {
    ctx.addIssue({ code: "custom", path: ["url"], message: "유튜브 영상이나 인스타그램 게시물 주소를 넣어 주세요" });
    return z.NEVER;
  }
  return { type, url: row.url };
});

// 목록 이름으로 감싸 오류 키가 서버와 같게("credits[0].sourceUrl") 나온다.
const SCHEMAS = {
  credits: z.object({ credits: z.array(creditRow) }),
  sources: z.object({ sources: z.array(sourceRow) }),
  embeds: z.object({ embeds: z.array(embedRow) }),
};

export const LIST_NAMES: ListName[] = ["credits", "sources", "embeds"];

export function toRows(place: AdminPlace): ListRows {
  return {
    credits: place.credits.map((c) => ({
      key: rowKey(),
      makerId: c.makerId,
      makerName: c.maker.name,
      role: c.role,
      verified: c.verified,
      sourceUrl: c.sourceUrl ?? "",
    })),
    sources: place.sources.map((s) => ({ key: rowKey(), title: s.title, url: s.url, type: s.type })),
    embeds: place.embeds.map((e) => ({ key: rowKey(), url: e.url })),
  };
}

// 저장된 목록을 PUT 본문 모양으로(키 순서도 스키마 출력과 같다).
export function savedLists(place: AdminPlace): PlaceLists {
  return {
    credits: place.credits.map((c) => ({ makerId: c.makerId, role: c.role, verified: c.verified, sourceUrl: c.sourceUrl })),
    sources: place.sources.map((s) => ({ title: s.title, url: s.url, type: s.type })),
    embeds: place.embeds.map((e) => ({ type: e.type, url: e.url })),
  };
}

// 아무것도 입력하지 않은 줄. 저장 때 보내지 않고 오류도 내지 않는다(반쯤 채운 줄만 오류).
const BLANK: { [K in ListName]: (row: ListRows[K][number]) => boolean } = {
  credits: (row) => row.makerId === null && !row.makerName.trim() && !row.role && !row.verified && !row.sourceUrl.trim(),
  sources: (row) => !row.title.trim() && !row.url.trim() && !row.type,
  embeds: (row) => !row.url.trim(),
};

export function withoutBlankRows(rows: ListRows): ListRows {
  return {
    credits: rows.credits.filter((row) => !BLANK.credits(row)),
    sources: rows.sources.filter((row) => !BLANK.sources(row)),
    embeds: rows.embeds.filter((row) => !BLANK.embeds(row)),
  };
}

export type ListCheck = {
  // 형식이 맞고 저장된 것과 다른 목록만. 임시저장이 이것만 PUT한다.
  changes: Partial<PlaceLists>;
  dirty: Record<ListName, boolean>;
  errors: Record<string, string>;
};

// 입력을 저장할 때처럼 정규화해 저장된 목록과 비교한다. 형식이 틀린 목록은 저장된 값일 수 없으니 변경이다.
export function checkLists(rows: ListRows, place: AdminPlace): ListCheck {
  const saved = savedLists(place);
  const changes: Partial<PlaceLists> = {};
  const dirty = { credits: false, sources: false, embeds: false };
  const errors: Record<string, string> = {};
  for (const name of LIST_NAMES) {
    // 빈 줄을 뺀 뒤 검사하고, 오류 키의 번호는 화면 줄 번호로 되돌린다.
    const kept = rows[name].flatMap((row, i) => ((BLANK[name] as (r: typeof row) => boolean)(row) ? [] : [i]));
    const result = SCHEMAS[name].safeParse({ [name]: kept.map((i) => rows[name][i]) });
    if (!result.success) {
      dirty[name] = true;
      for (const [key, message] of Object.entries(toFieldErrors(result.error.issues))) {
        errors[key.replace(/^(\w+)\[(\d+)\]/, (_, list, n) => `${list}[${kept[Number(n)]}]`)] = message;
      }
      continue;
    }
    const body = (result.data as PlaceLists)[name];
    if (JSON.stringify(body) !== JSON.stringify(saved[name])) {
      dirty[name] = true;
      Object.assign(changes, { [name]: body });
    }
  }
  return { changes, dirty, errors };
}

export type SaveStep = "edit" | ListName;

// 임시저장의 요청 순서. 공개 페이지가 있는 공간은 요청마다 공개 검증을 거치므로, "머물러 본 달을 비우며 출처를 더하기"는
// 출처가 먼저, "출처를 모두 빼며 머물러 본 달을 채우기"는 기본 값(PATCH)이 먼저여야 한다.
export function saveOrder(hasEdit: boolean, lists: Partial<PlaceLists>): SaveStep[] {
  const sourcesFirst = lists.sources !== undefined && lists.sources.length > 0;
  const steps: SaveStep[] = [];
  if (sourcesFirst) steps.push("sources");
  if (hasEdit) steps.push("edit");
  if (lists.credits) steps.push("credits");
  if (lists.sources && !sourcesFirst) steps.push("sources");
  if (lists.embeds) steps.push("embeds");
  return steps;
}

// 끌어 놓기와 ↑↓가 같이 쓴다.
export function move<T>(list: readonly T[], from: number, to: number): T[] {
  if (from === to || from < 0 || to < 0 || from >= list.length || to >= list.length) return [...list];
  const next = [...list];
  const [item] = next.splice(from, 1);
  next.splice(to, 0, item);
  return next;
}
