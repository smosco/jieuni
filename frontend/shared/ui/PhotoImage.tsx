import type { Photo } from "@/shared/api/types";

// 서버가 준 WebP 두 크기를 그대로 쓴다. Next 이미지 최적화는 쓰지 않는다(이미 변환된 WebP이고, Vercel 무료 변환 한도를 쓰지 않으려고).
export function PhotoImage({
  photo,
  sizes,
  className,
  priority = false,
}: {
  photo: Photo;
  sizes: string;
  className?: string;
  priority?: boolean;
}) {
  return (
    // eslint-disable-next-line @next/next/no-img-element -- 위 주석 이유로 <img srcset>을 쓴다
    <img
      className={className}
      src={photo.url.w800}
      srcSet={`${photo.url.w800} 800w, ${photo.url.w1600} 1600w`}
      sizes={sizes}
      width={photo.width}
      height={photo.height}
      alt={photo.alt}
      loading={priority ? "eager" : "lazy"}
      fetchPriority={priority ? "high" : undefined}
    />
  );
}
