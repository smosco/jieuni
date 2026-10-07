import { timingSafeEqual } from "node:crypto";
import { revalidateTag } from "next/cache";

// Spring이 공개·수정 커밋 뒤에 부른다. 파일 라우트라 next.config.ts의 /api rewrites보다 먼저 잡힌다.
// { expire: 0 }: 다음 요청이 옛 페이지 없이 새로 만든다. 'max'는 옛 페이지를 먼저 보여줘 공개 직후 새로고침에 반영되지 않는다.
export async function POST(request: Request) {
  if (!isValidSecret(request.headers.get("x-revalidate-secret"))) {
    return Response.json({ message: "재생성 비밀이 맞지 않아요" }, { status: 401 });
  }

  const body = (await request.json().catch(() => null)) as { tags?: unknown } | null;
  const tags = body?.tags;
  if (!Array.isArray(tags) || tags.length === 0 || !tags.every((t) => typeof t === "string")) {
    return Response.json({ message: "tags는 문자열 배열이어야 해요" }, { status: 400 });
  }

  for (const tag of tags) {
    revalidateTag(tag, { expire: 0 });
  }
  return Response.json({ revalidated: tags });
}

// 비교 시간으로 비밀을 추측하지 못하게 한다. timingSafeEqual은 길이가 다르면 던지므로 먼저 거른다.
function isValidSecret(given: string | null): boolean {
  const secret = process.env.REVALIDATE_SECRET;
  if (!secret || given === null) {
    return false;
  }
  const a = Buffer.from(given);
  const b = Buffer.from(secret);
  return a.length === b.length && timingSafeEqual(a, b);
}
