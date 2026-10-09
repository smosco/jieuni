import Link from "next/link";
import { AdminHead } from "./AdminHead";
import styles from "./Admin.module.css";

// 편성 현황·대기 제보·작성 중·AI 비용 블록은 대시보드 API가 생긴 뒤에 붙인다.
export function AdminHome() {
  return (
    <>
      <AdminHead title="관리자" />
      <div className={`label-md ${styles.shortcuts}`}>
        <Link href="/admin/places" className={styles.shortcut}>
          공간 목록 →
        </Link>
        <Link href="/admin/places/new" className={styles.shortcut}>
          새 공간 →
        </Link>
      </div>
    </>
  );
}
