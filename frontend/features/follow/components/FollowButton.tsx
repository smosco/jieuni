"use client";

import { useRequireLogin } from "@/features/auth/useRequireLogin";
import styles from "./FollowButton.module.css";

// 지금은 누르면 팔로우만 보낸다(비로그인이면 로그인 시트). 팔로잉 표시와 해제는 팔로우 상태를 붙일 때 연결한다.
export function FollowButton({
  makerId,
  following = false,
  makerName,
  onPhoto = false,
  onLeave,
}: {
  makerId: number;
  following?: boolean;
  makerName: string;
  onPhoto?: boolean;
  onLeave?: () => void;
}) {
  const requireLogin = useRequireLogin();

  return (
    <button
      type="button"
      className={`label-sm ${styles.button} ${onPhoto ? styles.onPhoto : ""} ${following ? styles.following : ""}`}
      aria-pressed={following}
      aria-label={`${makerName} ${following ? "팔로잉" : "팔로우"}`}
      onClick={() => requireLogin({ action: "follow", makerId }, onLeave)}
    >
      {following ? "팔로잉" : "팔로우"}
    </button>
  );
}
