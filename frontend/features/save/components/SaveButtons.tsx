"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRequireLogin } from "@/features/auth/useRequireLogin";
import { currentSaveStatus, useSaveMutation, useSaveStatus } from "../queries";
import { nextSaveStatus, type SaveStatus } from "../types";
import styles from "./SaveButtons.module.css";

const BUTTONS: { status: SaveStatus; label: string }[] = [
  { status: "want", label: "가고 싶어요" },
  { status: "been", label: "다녀왔어요" },
];

export function SaveButtons({
  placeId,
  onPhoto = false,
  onLeave,
}: {
  placeId: number;
  onPhoto?: boolean;
  onLeave?: () => void;
}) {
  const status = useSaveStatus(placeId) ?? null;
  const { mutateAsync } = useSaveMutation(placeId);
  const queryClient = useQueryClient();
  const requireLogin = useRequireLogin();

  return (
    <div className={styles.group}>
      {BUTTONS.map((button) => (
        <button
          key={button.status}
          type="button"
          className={`label-md ${styles.button} ${onPhoto ? styles.onPhoto : ""} ${status === button.status ? styles.on : ""}`}
          aria-pressed={status === button.status}
          onClick={() => {
            const next = nextSaveStatus(currentSaveStatus(queryClient, placeId), button.status);
            requireLogin({ action: { action: "save", placeId, status: button.status }, onLeave, resume: next !== null }, () =>
              mutateAsync(next),
            );
          }}
        >
          {button.label}
        </button>
      ))}
    </div>
  );
}
