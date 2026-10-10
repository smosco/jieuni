import type { ReactNode } from "react";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { STATUS_LABEL, updatedAtLabel } from "../labels";
import type { AdminPlace } from "../types";
import styles from "./PlaceForm.module.css";

// 사진은 아직 이 폼에서 고칠 수 없다. 지금 있는 것과 공개 검증 메시지만 보인다.

function Block({ title, errors, children }: { title: string; errors: string[]; children: ReactNode }) {
  return (
    <section className={styles.group}>
      <SectionLabel>{title}</SectionLabel>
      {children}
      <p className={`label-sm ${styles.later}`}>여기서는 아직 고칠 수 없어요</p>
      {errors.map((error) => (
        <p key={error} className={`body-sm ${styles.error}`}>
          {error}
        </p>
      ))}
    </section>
  );
}

function Lines({ items }: { items: { key: string | number; main: string; sub?: string }[] }) {
  if (items.length === 0) return <p className={`body-sm ${styles.none}`}>아직 없어요</p>;
  return (
    <ul className={styles.lines}>
      {items.map((item) => (
        <li key={item.key} className={styles.line}>
          <span className="body-md">{item.main}</span>
          {item.sub && <span className={`label-sm ${styles.sub}`}>{item.sub}</span>}
        </li>
      ))}
    </ul>
  );
}

// errors의 키 중 이 묶음 것: "photos"와 "photos[0].alt"처럼 같은 이름으로 시작하는 것.
function pick(errors: Record<string, string>, ...names: string[]): string[] {
  return Object.entries(errors)
    .filter(([key]) => names.some((name) => key === name || key.startsWith(`${name}[`)))
    .map(([key, message]) => (key.includes("[") ? `${photoNumber(key)} ${message}` : message));
}

function photoNumber(key: string): string {
  const index = Number(key.match(/\[(\d+)\]/)?.[1]);
  return `사진 ${index + 1}:`;
}

export function PhotosBlock({ place, errors }: { place: AdminPlace; errors: Record<string, string> }) {
  return (
    <Block title="사진" errors={pick(errors, "photos")}>
      <Lines
        items={place.photos.map((photo) => ({
          key: photo.id,
          main: photo.alt || "설명 없음",
          sub: photo.credit ? `사진: ${photo.credit}` : "출처 이름 없음",
        }))}
      />
    </Block>
  );
}

export function StatusBlock({ place }: { place: AdminPlace }) {
  const suggested = place.suggestedBy || place.suggestionReason || place.suggestionLink || place.suggestionMakerHint;
  return (
    <section className={styles.group}>
      <SectionLabel>상태</SectionLabel>
      <dl className={styles.facts}>
        <dt className={`label-sm ${styles.sub}`}>지금</dt>
        <dd className="body-md">{STATUS_LABEL[place.status]}</dd>
        {place.publishedAt && (
          <>
            <dt className={`label-sm ${styles.sub}`}>처음 공개</dt>
            <dd className="body-md">{updatedAtLabel(place.publishedAt)}</dd>
          </>
        )}
        <dt className={`label-sm ${styles.sub}`}>마지막 수정</dt>
        <dd className="body-md">{updatedAtLabel(place.updatedAt)}</dd>
        {suggested && (
          <>
            <dt className={`label-sm ${styles.sub}`}>제보한 사람</dt>
            <dd className="body-md">{place.suggestedBy?.nickname ?? "탈퇴한 사용자"}</dd>
          </>
        )}
        {place.suggestionReason && (
          <>
            <dt className={`label-sm ${styles.sub}`}>오래 생각나는 이유</dt>
            <dd className="body-md">{place.suggestionReason}</dd>
          </>
        )}
        {place.suggestionLink && (
          <>
            <dt className={`label-sm ${styles.sub}`}>참고 링크</dt>
            <dd className="body-md">
              <a href={place.suggestionLink} target="_blank" rel="noreferrer" className={styles.link}>
                {place.suggestionLink}
              </a>
            </dd>
          </>
        )}
        {place.suggestionMakerHint && (
          <>
            <dt className={`label-sm ${styles.sub}`}>만든 사람 힌트</dt>
            <dd className="body-md">{place.suggestionMakerHint}</dd>
          </>
        )}
      </dl>
    </section>
  );
}
