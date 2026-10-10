import Link from "next/link";
import styles from "./Admin.module.css";

// 비로그인·일반 사용자에게는 관리자 화면이 있다는 것을 드러내지 않는다.
export function AdminNotFound() {
  return (
    <div className={styles.notFound}>
      <p className={`body-md ${styles.notFoundText}`}>찾을 수 없는 페이지예요</p>
      <Link href="/" className={`label-md ${styles.notFoundLink}`}>
        홈으로 →
      </Link>
    </div>
  );
}
