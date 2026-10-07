"use client";

import styles from "./FollowButton.module.css";

// DESIGN 4절 FollowButton. 이 조각에서는 비로그인 상태("팔로우")만 그린다.
// 누르면 로그인 시트(F6), 팔로우 상태는 F7에서 붙인다.
export function FollowButton({ following = false, makerName }: { following?: boolean; makerName: string }) {
  return (
    <button
      type="button"
      className={`label-sm ${styles.button} ${following ? styles.following : ""}`}
      aria-pressed={following}
      aria-label={`${makerName} ${following ? "팔로잉" : "팔로우"}`}
    >
      {following ? "팔로잉" : "팔로우"}
    </button>
  );
}
