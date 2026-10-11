import { NotFoundView } from "@/shared/ui/StatusView";
import { Shell } from "@/shared/ui/Shell";

// 어느 경로에도 맞지 않는 주소. 셸 그룹 밖이라 셸을 직접 그린다(셸 그룹의 notFound()는 (shell)/not-found).
export default function NotFound() {
  return (
    <Shell>
      <NotFoundView />
    </Shell>
  );
}
