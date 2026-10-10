import type { Embed, EmbedType } from "@/shared/api/types";

// 공개 게시물 URL → iframe 주소와 썸네일. 모르는 모양이면 null(그 임베드는 그리지 않는다).
export function embedSource(embed: Embed): { frameUrl: string; thumbnailUrl: string | null } | null {
  let url: URL;
  try {
    url = new URL(embed.url);
  } catch {
    return null;
  }

  if (embed.type === "youtube") {
    const id = url.hostname === "youtu.be"
      ? url.pathname.slice(1)
      : url.searchParams.get("v") ?? url.pathname.match(/^\/(?:shorts|embed)\/([^/]+)/)?.[1];
    if (!id) return null;
    return {
      frameUrl: `https://www.youtube-nocookie.com/embed/${id}?autoplay=1`,
      thumbnailUrl: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
    };
  }

  const code = url.pathname.match(/^\/(?:p|reel)\/([^/]+)/)?.[1];
  if (!code) return null;
  return { frameUrl: `https://www.instagram.com/p/${code}/embed`, thumbnailUrl: null };
}

// 주소의 도메인으로 임베드 종류를 정한다. 백엔드의 허용 도메인과 같다. 모르는 도메인이면 null.
const HOSTS: Record<string, EmbedType> = {
  "youtube.com": "youtube",
  "www.youtube.com": "youtube",
  "m.youtube.com": "youtube",
  "youtu.be": "youtube",
  "instagram.com": "instagram",
  "www.instagram.com": "instagram",
};

export function embedTypeOf(url: string): EmbedType | null {
  try {
    return HOSTS[new URL(url).hostname.toLowerCase()] ?? null;
  } catch {
    return null;
  }
}
