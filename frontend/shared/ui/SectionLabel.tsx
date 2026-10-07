import styles from "./SectionLabel.module.css";

export function SectionLabel({ children }: { children: string }) {
  return <h2 className={`label-sm ${styles.label}`}>{children}</h2>;
}
