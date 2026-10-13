import type { ReactNode } from "react";
import { BackButton } from "@/shared/ui/BackButton";
import styles from "./InfoPage.module.css";

export function InfoPage({
  label,
  title,
  effectiveDate,
  children,
}: {
  label: string;
  title: string;
  effectiveDate?: string;
  children: ReactNode;
}) {
  return (
    <article>
      <header className={styles.head}>
        <div className={styles.back}>
          <BackButton onPhoto={false} />
        </div>
        <p className={`label-sm ${styles.label}`}>{label}</p>
        <h1 className={`display-lg ${styles.title}`}>{title}</h1>
        {effectiveDate && <p className={`body-sm ${styles.date}`}>시행일 {effectiveDate}</p>}
      </header>
      <div className={styles.body}>{children}</div>
    </article>
  );
}

export function InfoSection({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className={styles.section}>
      <h2 className={`display-md ${styles.sectionTitle}`}>{title}</h2>
      {children}
    </section>
  );
}

export function P({ children }: { children: ReactNode }) {
  return <p className={`body-md ${styles.text}`}>{children}</p>;
}

export function List({ items }: { items: ReactNode[] }) {
  return (
    <ul className={`body-md ${styles.list}`}>
      {items.map((item, i) => (
        <li key={i}>{item}</li>
      ))}
    </ul>
  );
}
