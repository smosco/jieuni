import { notFound } from "next/navigation";

// 공개 slug 형식(SPEC 3절: 영문 소문자·숫자·하이픈). 형식이 다르면 백엔드를 부르지 않고 404.
export function assertSlug(slug: string): void {
  if (!/^[a-z0-9-]+$/.test(slug)) {
    notFound();
  }
}
