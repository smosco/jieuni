"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRequireLogin } from "@/features/auth/useRequireLogin";
import { currentFollowing, useFollowMutation, useFollowing } from "../queries";
import styles from "./FollowButton.module.css";

export function FollowButton({
  makerId,
  makerName,
  onPhoto = false,
  onLeave,
}: {
  makerId: number;
  makerName: string;
  onPhoto?: boolean;
  onLeave?: () => void;
}) {
  const following = useFollowing(makerId) ?? false;
  const { mutateAsync } = useFollowMutation(makerId);
  const queryClient = useQueryClient();
  const requireLogin = useRequireLogin();

  return (
    <button
      type="button"
      className={`label-sm ${styles.button} ${onPhoto ? styles.onPhoto : ""} ${following ? styles.following : ""}`}
      aria-pressed={following}
      aria-label={`${makerName} ${following ? "팔로잉" : "팔로우"}`}
      onClick={() => {
        const next = !currentFollowing(queryClient, makerId);
        requireLogin({ action: { action: "follow", makerId }, onLeave, resume: next }, () => mutateAsync(next));
      }}
    >
      {following ? "팔로잉" : "팔로우"}
    </button>
  );
}
