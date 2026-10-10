import { z } from "zod";
import type { MakerKind } from "@/shared/api/types";
import type { AdminMaker, MakerEdit } from "./types";

const KINDS = ["person", "studio", "owner"] as const satisfies readonly MakerKind[];
// 백엔드 검사와 같은 식
const SLUG = /^[a-z0-9]+(-[a-z0-9]+)*$/;
const HTTP_URL = /^https?:\/\/\S+$/;
const INSTAGRAM = /^[A-Za-z0-9._]{1,30}$/;

export type MakerFormValues = Omit<{ [K in keyof MakerEdit]: string }, "affiliationId"> & {
  affiliation: { id: number | null; name: string };
};

function text(max: number, message: string) {
  return z
    .string()
    .trim()
    .max(max, message)
    .transform((value) => value || null);
}

// 만든 사람 저장: 백엔드 POST·PATCH 요청의 검증과 같은 길이·문구.
export const makerSchema = z
  .object({
    slug: text(80, "주소(slug)는 80자까지 쓸 수 있어요").refine(
      (value) => value === null || SLUG.test(value),
      "영문 소문자·숫자·하이픈만 쓸 수 있어요",
    ),
    name: z.string().trim().min(1, "이름을 입력해 주세요").max(100, "이름은 100자까지 쓸 수 있어요"),
    kind: z
      .union([z.literal(""), z.enum(KINDS)])
      .refine((value) => value !== "", "종류를 골라 주세요")
      .transform((value) => value as MakerKind),
    region: text(50, "지역은 50자까지 쓸 수 있어요"),
    intro: text(200, "소개는 200자까지 쓸 수 있어요"),
    website: text(500, "링크는 500자까지 쓸 수 있어요").refine(
      (value) => value === null || HTTP_URL.test(value),
      "http로 시작하는 링크를 넣어 주세요",
    ),
    // 서버처럼 앞의 @는 지우고 아이디만 남긴다.
    instagram: z
      .string()
      .trim()
      .transform((value) => value.replace(/^@/, "").trim() || null)
      .refine((value) => value === null || INSTAGRAM.test(value), "인스타그램 아이디만 넣어 주세요"),
    affiliation: z.object({ id: z.number().nullable(), name: z.string() }),
  })
  .superRefine((values, ctx) => {
    // 이름을 고쳤는데 목록에서 고르지 않으면 소속이 비어 저장된다. 그걸 모르고 지나가지 않게 막는다.
    if (values.affiliation.id === null && values.affiliation.name.trim() !== "") {
      ctx.addIssue({ code: "custom", path: ["affiliationId"], message: "목록에서 골라 주세요" });
    }
  })
  .transform(
    (values): MakerEdit => ({
      slug: values.slug,
      name: values.name,
      kind: values.kind,
      region: values.region,
      intro: values.intro,
      website: values.website,
      instagram: values.instagram,
      affiliationId: values.affiliation.id,
    }),
  );

export function toMakerValues(maker: AdminMaker): MakerFormValues {
  const str = (value: string | null) => value ?? "";
  return {
    slug: str(maker.slug),
    name: maker.name,
    kind: maker.kind,
    region: str(maker.region),
    intro: str(maker.intro),
    website: str(maker.website),
    instagram: str(maker.instagram),
    affiliation: { id: maker.affiliation?.id ?? null, name: maker.affiliation?.name ?? "" },
  };
}

// 서버 값 → 요청 본문 모양(키 순서도 스키마 출력과 같다).
export function toMakerEdit(maker: AdminMaker): MakerEdit {
  return {
    slug: maker.slug,
    name: maker.name,
    kind: maker.kind,
    region: maker.region,
    intro: maker.intro,
    website: maker.website,
    instagram: maker.instagram,
    affiliationId: maker.affiliation?.id ?? null,
  };
}

// 공간 폼과 같이: 입력을 저장할 때처럼 정규화해 저장된 값과 비교한다. 형식이 틀리면 변경이다.
export function isMakerDirty(values: MakerFormValues, maker: AdminMaker): boolean {
  const parsed = makerSchema.safeParse(values);
  return !parsed.success || JSON.stringify(parsed.data) !== JSON.stringify(toMakerEdit(maker));
}
