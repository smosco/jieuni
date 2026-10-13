"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { useId, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { BottomSheet } from "@/shared/ui/BottomSheet";
import { PolicyLinks } from "@/shared/ui/PolicyLinks";
import { useToast } from "@/shared/ui/Toast";
import { changeNickname, deleteMe, logout } from "../api";
import { forgetUser } from "../cache";
import type { Me } from "../types";
import form from "@/shared/ui/Form.module.css";
import styles from "./MySpace.module.css";

export function AccountSheet({ open, me, onClose }: { open: boolean; me: Me; onClose: () => void }) {
  const titleId = useId();
  return (
    <BottomSheet open={open} onClose={onClose} labelledBy={titleId}>
      {/* 닫을 때마다 내용을 내려 입력과 탈퇴 확인을 처음 상태로 돌린다. */}
      {open && <Content titleId={titleId} me={me} onClose={onClose} />}
    </BottomSheet>
  );
}

function Content({ titleId, me, onClose }: { titleId: string; me: Me; onClose: () => void }) {
  const [confirming, setConfirming] = useState(false);
  const [leaving, setLeaving] = useState(false);
  const [nickname, setNickname] = useState(me.nickname);
  const [error, setError] = useState<string | null>(null);
  const queryClient = useQueryClient();
  const toast = useToast();
  const router = useRouter();

  async function saveNickname(e: FormEvent) {
    e.preventDefault();
    try {
      queryClient.setQueryData(["me"], await changeNickname(nickname.trim()));
      onClose();
      toast("닉네임을 바꿨어요");
    } catch (err) {
      setError(err instanceof ApiError && err.fieldErrors?.nickname ? err.fieldErrors.nickname : "바꾸지 못했어요. 다시 시도해 주세요");
    }
  }

  // 사용자별 캐시를 지우고 /api/me를 다시 받는다. 로그아웃 때 지워진 CSRF 쿠키도 이 응답으로 새로 받는다.
  async function signedOut() {
    forgetUser(queryClient);
    await queryClient.invalidateQueries({ queryKey: ["me"], exact: true });
  }

  async function signOut() {
    try {
      await logout();
    } catch {
      toast("로그아웃하지 못했어요. 다시 시도해 주세요");
      return;
    }
    onClose();
    await signedOut();
  }

  // 두 번 누르면 두 번째 요청이 이미 지운 세션으로 가서 실패 토스트가 뜬다.
  async function leave() {
    setLeaving(true);
    try {
      await deleteMe();
    } catch {
      setLeaving(false);
      toast("탈퇴하지 못했어요. 다시 시도해 주세요");
      return;
    }
    onClose();
    await signedOut();
    toast("탈퇴했어요");
    router.push("/");
  }

  if (confirming) {
    return (
      <>
        <h2 id={titleId} className={`display-md ${styles.sheetTitle}`}>
          탈퇴할까요?
        </h2>
        <p className={`body-md ${styles.sheetText}`}>
          저장한 공간과 팔로우는 바로 지워져요. 소개한 공간은 남지만 누가 소개했는지는 지워져요.
        </p>
        <div className={styles.sheetButtons}>
          <button type="button" className={`label-md ${form.primary}`} disabled={leaving} onClick={leave}>
            탈퇴하기
          </button>
          <button type="button" className={`label-md ${form.secondary}`} onClick={() => setConfirming(false)}>
            취소
          </button>
        </div>
      </>
    );
  }

  return (
    <>
      <h2 id={titleId} className={`display-md ${styles.sheetTitle}`}>
        설정
      </h2>
      <form className={styles.nicknameForm} onSubmit={saveNickname}>
        <label className={`label-sm ${styles.sub}`} htmlFor={`${titleId}-nickname`}>
          닉네임
        </label>
        <div className={styles.nicknameRow}>
          <input
            id={`${titleId}-nickname`}
            className={`body-md ${form.input}`}
            value={nickname}
            onChange={(e) => {
              setNickname(e.target.value);
              setError(null);
            }}
            aria-invalid={error !== null}
            aria-describedby={error ? `${titleId}-error` : undefined}
          />
          <button type="submit" className={`label-md ${styles.smallButton}`} disabled={nickname.trim() === me.nickname}>
            저장
          </button>
        </div>
        {error && (
          <p id={`${titleId}-error`} className={`body-sm ${form.error}`}>
            {error}
          </p>
        )}
      </form>
      <div className={styles.sheetButtons}>
        <button type="button" className={`label-md ${form.secondary}`} onClick={signOut}>
          로그아웃
        </button>
      </div>
      <button type="button" className={`label-md ${styles.textButton}`} onClick={() => setConfirming(true)}>
        탈퇴
      </button>
      <PolicyLinks onNavigate={onClose} />
    </>
  );
}
