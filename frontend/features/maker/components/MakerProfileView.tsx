import Link from "next/link";
import { FollowButton } from "@/features/follow/components/FollowButton";
import { subjectParticle } from "@/shared/lib/josa";
import { makerLabel } from "@/shared/lib/labels";
import { BackButton } from "@/shared/ui/BackButton";
import { PlaceGrid } from "@/shared/ui/PlaceTile";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import type { MakerProfile } from "../types";
import styles from "./MakerProfileView.module.css";

export function MakerProfileView({ maker }: { maker: MakerProfile }) {
  const label = makerLabel(maker.kind, maker.region);

  return (
    <article>
      <header className={styles.head}>
        <div className={styles.back}>
          <BackButton onPhoto={false} />
        </div>
        <p className={`label-sm ${styles.label}`}>{label}</p>
        <div className={styles.nameRow}>
          <h1 className={`display-xl ${styles.name}`}>{maker.name}</h1>
          <FollowButton makerId={maker.id} makerName={maker.name} />
        </div>
        {maker.intro && <p className={`body-md ${styles.intro}`}>{maker.intro}</p>}
        <MakerLinks maker={maker} />
      </header>

      <div className={styles.sectionLabel}>
        <SectionLabel>{`${maker.name}${subjectParticle(maker.name)} 만든 공간`}</SectionLabel>
      </div>
      <PlaceGrid places={maker.places} variant="bleed" />
    </article>
  );
}

// 있는 것만 " · "로 잇는다. 셋 다 없으면 줄을 그리지 않는다.
function MakerLinks({ maker }: { maker: MakerProfile }) {
  const items = [];
  if (maker.website) {
    items.push(
      <a key="web" href={maker.website} target="_blank" rel="noopener noreferrer">
        {displayUrl(maker.website)}
      </a>,
    );
  }
  if (maker.instagram) {
    items.push(
      <a key="ig" href={`https://www.instagram.com/${maker.instagram}/`} target="_blank" rel="noopener noreferrer">
        @{maker.instagram}
      </a>,
    );
  }
  if (maker.affiliation) {
    items.push(
      maker.affiliation.slug ? (
        <Link key="aff" href={`/makers/${maker.affiliation.slug}`}>
          소속 {maker.affiliation.name} →
        </Link>
      ) : (
        <span key="aff">소속 {maker.affiliation.name}</span>
      ),
    );
  }
  if (items.length === 0) return null;

  return (
    <p className={`body-sm ${styles.links}`}>
      {items.flatMap((item, i) => (i === 0 ? [item] : [<span key={`sep${i}`} aria-hidden="true"> · </span>, item]))}
    </p>
  );
}

// 링크 글자는 짧게: 프로토콜·www.·끝의 /를 뺀다.
function displayUrl(url: string): string {
  return url.replace(/^https?:\/\//, "").replace(/^www\./, "").replace(/\/$/, "");
}
