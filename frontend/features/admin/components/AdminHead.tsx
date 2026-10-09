import Link from "next/link";
import form from "@/shared/ui/Form.module.css";
import styles from "./Admin.module.css";

export function AdminHead({ title, newPlace = false }: { title: string; newPlace?: boolean }) {
  return (
    <header className={styles.head}>
      <div className={styles.headText}>
        <p className={`label-sm ${styles.eyebrow}`}>ADMIN</p>
        <h1 className={`display-lg ${styles.title}`}>{title}</h1>
      </div>
      {newPlace && (
        <Link href="/admin/places/new" className={`label-md ${form.secondary} ${styles.linkButton}`}>
          새 공간
        </Link>
      )}
    </header>
  );
}
