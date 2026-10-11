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
      srcSet={srcSet(photo)}
      sizes={sizes}
      width={photo.width}
      height={photo.height}
      alt={photo.alt}
      loading={priority ? "eager" : "lazy"}
      fetchPriority={priority ? "high" : undefined}
    />
  );
}

// width는 큰 파일의 실제 너비다. 작은 사진은 키우지 않고 올려서 파일 너비가 이름(1600·800)보다 작을 수 있다.
// 두 파일 너비가 같으면(800 이하) 하나만 적는다.
function srcSet(photo: Photo): string {
  const small = Math.min(photo.width, 800);
  return small === photo.width
    ? `${photo.url.w800} ${small}w`
    : `${photo.url.w800} ${small}w, ${photo.url.w1600} ${photo.width}w`;
}
