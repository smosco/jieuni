import { notFound } from "next/navigation";
import { ApiError } from "@/shared/api/client";
import { serverFetch } from "@/shared/api/server";
import { assertSlug } from "@/shared/lib/slug";
import type { MakerProfile } from "./types";

// 페이지·메타데이터·공유 카드가 같이 부른다. 같은 렌더 안의 같은 fetch는 Next가 한 번만 보낸다.
export async function loadMaker(slug: string): Promise<MakerProfile> {
  assertSlug(slug);
  return serverFetch<MakerProfile>(`/api/makers/${encodeURIComponent(slug)}`, { tags: [`maker:${slug}`] }).catch(
    (e: unknown) => {
      if (e instanceof ApiError && e.status === 404) notFound();
      throw e;
    },
  );
}
