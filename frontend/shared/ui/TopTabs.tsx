import Link from "next/link";
import styles from "./TopTabs.module.css";

// 둘러보기·내 공간의 상단 탭. 탭마다 주소가 달라 뒤로 가기로 돌아갈 수 있다.
export function TopTabs({ items }: { items: { href: string; label: string; active: boolean }[] }) {
  return (
    <nav className={`label-md ${styles.tabs}`}>
      {items.map((item) => (
        <Link
          key={item.href}
          href={item.href}
          className={`${styles.tab} ${item.active ? styles.active : ""}`}
          aria-current={item.active ? "page" : undefined}
        >
          {item.label}
        </Link>
      ))}
    </nav>
  );
}
