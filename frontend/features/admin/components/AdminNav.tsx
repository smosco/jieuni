"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useConfirmLeave } from "./LeaveGuard";
import styles from "./Admin.module.css";

// href가 없는 항목은 아직 화면이 없다(AI 기록은 AI 초안 연동 뒤).
const MENU: { href?: string; label: string }[] = [
  { href: "/admin/places", label: "공간" },
  { href: "/admin/makers", label: "만든 사람" },
  { href: "/admin/picks", label: "편성" },
  { href: "/admin/suggestions", label: "제보" },
  { label: "AI 기록" },
];

export function AdminNav() {
  const pathname = usePathname();
  const confirmLeave = useConfirmLeave();
  const guard = (e: { preventDefault: () => void }) => {
    if (!confirmLeave()) e.preventDefault();
  };

  return (
    <nav className={styles.nav} aria-label="관리자 메뉴">
      <Link href="/admin" className={styles.brand} onNavigate={guard}>
        <span className={`label-sm ${styles.eyebrow}`}>ADMIN</span>
        <span className={`display-md ${styles.brandName}`}>공간의 지은이</span>
      </Link>
      <ul className={`label-md ${styles.menu}`}>
        {MENU.map((item) => {
          if (!item.href) {
            return (
              <li key={item.label}>
                <span className={`${styles.menuItem} ${styles.menuOff}`} aria-disabled="true">
                  {item.label}
                </span>
              </li>
            );
          }
          const active = pathname.startsWith(item.href);
          return (
            <li key={item.href}>
              <Link
                href={item.href}
                className={`${styles.menuItem} ${active ? styles.menuOn : ""}`}
                aria-current={active ? "page" : undefined}
                onNavigate={guard}
              >
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
      <Link href="/" className={`label-md ${styles.siteLink}`} onNavigate={guard}>
        사이트로 →
      </Link>
    </nav>
  );
}
