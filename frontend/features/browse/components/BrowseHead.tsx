import { TopTabs } from "@/shared/ui/TopTabs";
import styles from "./Browse.module.css";

export function BrowseHead({ active }: { active: "places" | "makers" }) {
  return (
    <>
      <header className={styles.head}>
        <p className={`label-sm ${styles.eyebrow}`}>BROWSE</p>
        <h1 className={`display-lg ${styles.title}`}>둘러보기</h1>
      </header>
      <TopTabs
        items={[
          { href: "/places", label: "공간", active: active === "places" },
          { href: "/makers", label: "만든 사람", active: active === "makers" },
        ]}
      />
    </>
  );
}
