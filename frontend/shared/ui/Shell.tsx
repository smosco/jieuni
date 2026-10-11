import type { ReactNode } from "react";
import { TabBar } from "./TabBar";
import styles from "./Shell.module.css";

// 공개 화면의 틀(가운데 480px + 하단 탭). 404·오류 화면도 같은 틀에 그린다.
export function Shell({ children }: { children: ReactNode }) {
  return (
    <>
      <div className={styles.shell}>{children}</div>
      <TabBar />
    </>
  );
}
