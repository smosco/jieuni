import Link from "next/link";
import { Fragment } from "react";
import styles from "./PolicyLinks.module.css";

const LINKS = [
  { href: "/about", label: "소개" },
  { href: "/terms", label: "이용약관" },
  { href: "/privacy", label: "개인정보처리방침" },
];

export function PolicyLinks({ onNavigate }: { onNavigate?: () => void }) {
  return (
    <nav className={`body-sm ${styles.row}`} aria-label="서비스 안내">
      {LINKS.map((link, i) => (
        <Fragment key={link.href}>
          {i > 0 && <span aria-hidden="true">·</span>}
          <Link href={link.href} className={styles.link} onClick={onNavigate}>
            {link.label}
          </Link>
        </Fragment>
      ))}
    </nav>
  );
}
