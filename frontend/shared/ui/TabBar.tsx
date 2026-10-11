"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useMe } from "@/features/me/queries";
import styles from "./TabBar.module.css";

const TABS = [
  { href: "/", label: "오늘", match: (p: string) => p === "/" },
  { href: "/places", label: "둘러보기", match: (p: string) => p.startsWith("/places") || p.startsWith("/makers") },
  { href: "/me", label: "내 공간", match: (p: string) => p.startsWith("/me") },
] as const;

export function TabBar() {
  const pathname = usePathname();
  // 비로그인은 null, 그 밖의 오류는 점을 안 찍는 것으로 끝낸다.
  const { data: me } = useMe();

  return (
    <nav className={styles.bar} aria-label="주요 화면">
      {TABS.map((tab) => {
        const active = tab.match(pathname);
        return (
          <Link
            key={tab.href}
            href={tab.href}
            className={`label-md hit-area ${styles.tab} ${active ? styles.active : ""}`}
            aria-current={active ? "page" : undefined}
          >
            <span className={styles.label}>{tab.label}</span>
            {tab.href === "/me" && me?.hasNewFromFollowing && (
              <span className={styles.dot} aria-label="새 공간 있음" />
            )}
          </Link>
        );
      })}
    </nav>
  );
}
