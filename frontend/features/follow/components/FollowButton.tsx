"use client";

import styles from "./FollowButton.module.css";

// 아직 로그인이 없어 비로그인 상태("팔로우")만 그린다. 누르면 로그인 시트, 팔로우 상태는 로그인을 붙일 때 연결한다.
export function FollowButton({
  following = false,
  makerName,
  onPhoto = false,
}: {
  following?: boolean;
  makerName: string;
  onPhoto?: boolean;
}) {
  return (
    <button
      type="button"
      className={`label-sm ${styles.button} ${onPhoto ? styles.onPhoto : ""} ${following ? styles.following : ""}`}
      aria-pressed={following}
      aria-label={`${makerName} ${following ? "팔로잉" : "팔로우"}`}
    >
      {following ? "팔로잉" : "팔로우"}
    </button>
  );
}
