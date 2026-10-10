import Link from "next/link";
import form from "@/shared/ui/Form.module.css";
import styles from "./Admin.module.css";

export function AdminHead({ title, status, newPlace = false }: { title: string; status?: string; newPlace?: boolean }) {
  return (
    <header className={styles.head}>
      <div className={styles.headText}>
        <p className={`label-sm ${styles.eyebrow}`}>ADMIN</p>
        <div className={styles.titleRow}>
          <h1 className={`display-lg ${styles.title}`}>{title}</h1>
          {status && <p className={`label-sm ${styles.status}`}>{status}</p>}
        </div>
      </div>
      {newPlace && (
        <Link href="/admin/places/new" className={`label-md ${form.secondary} ${styles.linkButton}`}>
          새 공간
        </Link>
      )}
    </header>
  );
}
