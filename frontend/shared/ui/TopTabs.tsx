import Link from "next/link";
import styles from "./TopTabs.module.css";

// 둘러보기·내 공간·관리자 목록의 상단 탭. 탭마다 주소가 달라 뒤로 가기로 돌아갈 수 있다.
// className은 놓이는 자리에 맞춘 여백·줄 바꿈을 바깥 nav에 더할 때 쓴다.
export function TopTabs({
  items,
  className,
}: {
  items: { href: string; label: string; active: boolean }[];
  className?: string;
}) {
  return (
    <nav className={["label-md", styles.tabs, className].filter(Boolean).join(" ")}>
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
