"use client";

import { useState } from "react";
import type { Photo } from "@/shared/api/types";
import { BackButton } from "@/shared/ui/BackButton";
import { PhotoImage } from "@/shared/ui/PhotoImage";
import type { Embed } from "@/shared/api/types";
import { embedSource } from "@/shared/lib/embed";
import styles from "./PlaceGallery.module.css";

// 가로 넘기기는 라이브러리 없이 scroll-snap. 사진 뒤에 임베드가 이어진다.
export function PlaceGallery({ photos, embeds, placeName }: { photos: Photo[]; embeds: Embed[]; placeName: string }) {
  const frames = embeds.flatMap((embed) => {
    const source = embedSource(embed);
    return source ? [{ ...source, type: embed.type, url: embed.url }] : [];
  });
  const total = photos.length + frames.length;
  const [index, setIndex] = useState(0);

  function onScroll(event: React.UIEvent<HTMLDivElement>) {
    const el = event.currentTarget;
    setIndex(Math.round(el.scrollLeft / el.clientWidth));
  }

  return (
    <div className={styles.gallery}>
      {total > 0 && (
        <div className={styles.track} onScroll={onScroll} aria-label={`${placeName} 사진`}>
          {photos.map((photo, i) => (
            <figure key={photo.url.w800} className={styles.slide}>
              <PhotoImage photo={photo} sizes="(max-width: 480px) 100vw, 480px" className={styles.photo} priority={i === 0} />
              <figcaption className={`label-sm ${styles.credit}`}>
                사진:{" "}
                {photo.sourceUrl ? (
                  <a href={photo.sourceUrl} target="_blank" rel="noopener noreferrer">
                    {photo.credit}
                  </a>
                ) : (
                  photo.credit
                )}
              </figcaption>
            </figure>
          ))}
          {frames.map((frame) => (
            <div key={frame.url} className={styles.slide}>
              <EmbedSlide {...frame} />
            </div>
          ))}
        </div>
      )}
      <div className={styles.back}>
        <BackButton onPhoto={total > 0} />
      </div>
      {total > 1 && (
        <p className={`label-sm ${styles.counter}`} aria-hidden="true">
          {index + 1} / {total}
        </p>
      )}
    </div>
  );
}

// 처음에는 썸네일만, 누르면 iframe을 불러온다(외부 iframe이 첫 화면 로딩을 늦추지 않게).
function EmbedSlide({ type, frameUrl, thumbnailUrl }: { type: Embed["type"]; frameUrl: string; thumbnailUrl: string | null }) {
  const [open, setOpen] = useState(false);
  const label = type === "youtube" ? "영상 보기" : "인스타그램 게시물 보기";

  if (open) {
    return (
      <iframe
        className={styles.frame}
        src={frameUrl}
        title={label}
        allow="autoplay; encrypted-media; picture-in-picture"
        allowFullScreen
      />
    );
  }
  return (
    <button type="button" className={styles.embedButton} onClick={() => setOpen(true)}>
      {thumbnailUrl && (
        // eslint-disable-next-line @next/next/no-img-element -- 외부 썸네일이라 변환 한도를 쓰는 Next 이미지 최적화를 거치지 않는다
        <img className={styles.photo} src={thumbnailUrl} alt="" loading="lazy" />
      )}
      <span className={`label-md ${styles.embedLabel}`}>{label}</span>
    </button>
  );
}
