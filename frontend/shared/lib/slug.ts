import { notFound } from "next/navigation";

// 공개 slug는 영문 소문자·숫자·하이픈뿐이다. 형식이 다르면 있을 수 없는 주소라 백엔드를 부르지 않고 404.
export function assertSlug(slug: string): void {
  if (!/^[a-z0-9-]+$/.test(slug)) {
    notFound();
  }
}
