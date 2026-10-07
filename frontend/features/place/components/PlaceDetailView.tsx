import Link from "next/link";
import { FollowButton } from "@/features/follow/components/FollowButton";
import { SaveButtons } from "@/features/save/components/SaveButtons";
import { KIND_LABEL, ROLE_LABEL } from "@/shared/lib/labels";
import { PlaceGrid } from "@/shared/ui/PlaceTile";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { ACCESS_LABEL, ENCOUNTER_LABEL, yearMonth } from "../labels";
import type { PlaceDetail } from "../types";
import { Essay } from "./Essay";
import { PlaceGallery } from "./PlaceGallery";
import { CopyAddressButton, ShareButton } from "./ShareButton";
import styles from "./PlaceDetailView.module.css";

const SENSES = [
  { key: "light", label: "빛" },
  { key: "sound", label: "소리" },
  { key: "scent", label: "향" },
  { key: "touch", label: "촉감" },
] as const;

const STATUS_BAND = {
  paused: "지금은 쉬고 있어요",
  closed: "이제는 만날 수 없는 공간이에요",
} as const;

// 관리자 미리보기도 이 컴포넌트에 같은 응답을 넣어 공개 화면과 똑같이 보이게 한다.
export function PlaceDetailView({ place }: { place: PlaceDetail }) {
  const senses = SENSES.filter((sense) => place.senses[sense.key]);
  const meta = [place.region, KIND_LABEL[place.kind], place.access && ACCESS_LABEL[place.access]].filter(Boolean);
  const relatedTitle =
    place.credits.length === 1 ? `${place.credits[0].maker.name}의 다른 공간` : "만든 사람들의 다른 공간";

  return (
    <article>
      <PlaceGallery photos={place.photos} embeds={place.embeds} placeName={place.name} />
      {place.status !== "published" && <p className={`body-sm ${styles.band}`}>{STATUS_BAND[place.status]}</p>}

      <div className={styles.body}>
        <header className={styles.titleBlock}>
          <Encounter place={place} />
          <h1 className={`display-xl ${styles.name}`}>{place.name}</h1>
          <p className={`body-sm ${styles.meta}`}>{meta.join(" · ")}</p>
          <div className={styles.actions}>
            {place.status !== "closed" && <SaveButtons />}
            <ShareButton title={place.name} />
          </div>
        </header>

        {place.impression && (
          <section>
            <SectionLabel>에디터의 감상</SectionLabel>
            <p className={`body-lg ${styles.impression}`}>{place.impression}</p>
          </section>
        )}

        {senses.length > 0 && (
          <section>
            <SectionLabel>감각</SectionLabel>
            <dl className={styles.rows}>
              {senses.map((sense) => (
                <div key={sense.key} className={styles.row}>
                  <dt className={`label-sm ${styles.senseLabel}`}>{sense.label}</dt>
                  <dd className="body-md">{place.senses[sense.key]}</dd>
                </div>
              ))}
            </dl>
          </section>
        )}

        <section>
          <SectionLabel>만든 사람들</SectionLabel>
          <ul className={styles.credits}>
            {place.credits.map((credit) => (
              <li key={credit.position} className={styles.credit}>
                <span className={`label-sm ${styles.role}`}>{ROLE_LABEL[credit.role]}</span>
                <span className={styles.who}>
                  {credit.maker.slug ? (
                    <Link href={`/makers/${credit.maker.slug}`} className="body-md">
                      {credit.maker.name}
                    </Link>
                  ) : (
                    <span className="body-md">{credit.maker.name}</span>
                  )}
                  <CreditStatus verified={credit.verified} sourceUrl={credit.sourceUrl} />
                </span>
                {/* 프로필이 없는 만든 사람은 팔로우해도 볼 곳이 없다 */}
                {credit.maker.slug && <FollowButton makerName={credit.maker.name} />}
              </li>
            ))}
          </ul>
          {/* 누르면 크레딧 알려주기 시트(제보 기능을 붙일 때 연결) */}
          <button type="button" className={`body-sm ${styles.suggest}`}>
            이 공간을 만드셨나요? 크레딧을 알려주세요 →
          </button>
        </section>

        {place.essay && (
          <section>
            <SectionLabel>에세이</SectionLabel>
            <Essay markdown={place.essay} />
          </section>
        )}

        <section>
          <SectionLabel>방문 정보</SectionLabel>
          <dl className={styles.rows}>
            {place.address && (
              <div className={`${styles.row} ${styles.visitRow}`}>
                <dt className={`label-sm ${styles.visitLabel}`}>주소</dt>
                <dd className={`body-md ${styles.address}`}>
                  <span>{place.address}</span>
                  <CopyAddressButton address={place.address} />
                </dd>
              </div>
            )}
            {place.access && (
              <div className={`${styles.row} ${styles.visitRow}`}>
                <dt className={`label-sm ${styles.visitLabel}`}>방문</dt>
                <dd className="body-md">{ACCESS_LABEL[place.access]}</dd>
              </div>
            )}
            {place.visitNote && (
              <div className={`${styles.row} ${styles.visitRow}`}>
                <dt className={`label-sm ${styles.visitLabel}`}>메모</dt>
                <dd className="body-md">{place.visitNote}</dd>
              </div>
            )}
            <div className={`${styles.row} ${styles.visitRow}`}>
              <dt className={`label-sm ${styles.visitLabel}`}>지도</dt>
              <dd className="body-md">
                <a href={place.mapLinks.naver} target="_blank" rel="noopener noreferrer">
                  네이버지도
                </a>
                {" · "}
                <a href={place.mapLinks.kakao} target="_blank" rel="noopener noreferrer">
                  카카오맵
                </a>
              </dd>
            </div>
          </dl>
        </section>

        {place.relatedPlaces.length > 0 && (
          <section>
            <SectionLabel>{relatedTitle}</SectionLabel>
            <PlaceGrid places={place.relatedPlaces} variant="inset" />
          </section>
        )}
      </div>
    </article>
  );
}

// verified면 "출처 확인됨"(출처가 있을 때만 링크), 아니면 "확인 중"
function CreditStatus({ verified, sourceUrl }: { verified: boolean; sourceUrl: string | null }) {
  const className = `body-sm ${styles.verified}`;
  if (!verified) return <span className={className}>확인 중</span>;
  if (!sourceUrl) return <span className={className}>출처 확인됨</span>;
  return (
    <a href={sourceUrl} target="_blank" rel="noopener noreferrer" className={className}>
      출처 확인됨
    </a>
  );
}

// 에디터가 다녀왔으면 머물러 봄 배지, 아니면 이 공간을 알게 된 첫 출처로 "○○으로 만남"
function Encounter({ place }: { place: PlaceDetail }) {
  if (place.visitedByEditorOn) {
    return (
      <p className={styles.encounter}>
        <span className={`label-sm ${styles.badge}`}>머물러 봄</span>
        <span className={`body-sm ${styles.visitedOn}`}>{yearMonth(place.visitedByEditorOn)}</span>
      </p>
    );
  }
  const source = place.sources[0];
  if (!source) return null;
  return (
    <p className={styles.encounter}>
      <a href={source.url} target="_blank" rel="noopener noreferrer" className={`body-sm ${styles.encounterLink}`}>
        {ENCOUNTER_LABEL[source.type]}
      </a>
    </p>
  );
}
