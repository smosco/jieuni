import Link from "next/link";
import type { PlaceTile as PlaceTileData } from "@/shared/api/types";
import { PhotoImage } from "./PhotoImage";
import styles from "./PlaceTile.module.css";

// DESIGN 4절 PlaceTile. 캡션은 API의 caption, 없으면 공간 이름.
export function PlaceTile({ place, sizes }: { place: PlaceTileData; sizes: string }) {
  const closed = place.status === "closed";
  const caption = place.caption ?? place.name;
  return (
    <Link href={`/places/${place.slug}`} className={`${styles.tile} ${closed ? styles.closed : ""}`}>
      {place.photo && <PhotoImage photo={place.photo} sizes={sizes} className={styles.photo} />}
      <span className={`label-sm ${styles.caption}`}>
        {caption}
        {closed && " · 폐업"}
      </span>
    </Link>
  );
}

// variant: inset = 본문 너비 안(상세 04, 사이 4px) · bleed = 화면 끝까지(프로필·둘러보기, 사이 1.5px)
export function PlaceGrid({
  places,
  variant,
}: {
  places: PlaceTileData[];
  variant: "inset" | "bleed";
}) {
  return (
    <ul className={`${styles.grid} ${styles[variant]}`}>
      {places.map((place) => (
        <li key={place.id}>
          <PlaceTile place={place} sizes="(max-width: 480px) 34vw, 160px" />
        </li>
      ))}
    </ul>
  );
}
