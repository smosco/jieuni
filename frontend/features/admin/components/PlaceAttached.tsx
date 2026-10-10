import { SectionLabel } from "@/shared/ui/SectionLabel";
import { STATUS_LABEL, updatedAtLabel } from "../labels";
import type { AdminPlace } from "../types";
import styles from "./PlaceForm.module.css";

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
