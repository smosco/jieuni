import { TabBar } from "@/shared/ui/TabBar";
import styles from "@/shared/ui/Shell.module.css";

// 공개 화면·내 공간·제보가 쓰는 셸. 관리자 화면(최대 960px, 탭 바 없음)은 이 그룹 밖에 둔다.
export default function ShellLayout({ children }: LayoutProps<"/">) {
  return (
    <>
      <div className={styles.shell}>{children}</div>
      <TabBar />
    </>
  );
}
