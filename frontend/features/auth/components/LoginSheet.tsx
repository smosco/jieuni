"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useId, useState } from "react";
import { useToast } from "@/shared/ui/Toast";
import { BottomSheet } from "@/shared/ui/BottomSheet";
import { devLogin } from "../api";
import { loginUrl } from "../login";
import { clearPending, savePending } from "../pendingAction";
import type { LoginRequest, PendingAction } from "../types";
import styles from "./LoginSheet.module.css";

const TITLE: Record<PendingAction["action"], string> = {
  save: "저장하려면 로그인이 필요해요",
  follow: "팔로우하려면 로그인이 필요해요",
  suggest: "소개하려면 로그인이 필요해요",
  me: "내 공간을 보려면 로그인이 필요해요",
};

const DEV_LOGIN = process.env.NODE_ENV === "development";

export function LoginSheet({
  request,
  onClose,
}: {
  request: LoginRequest | null;
  onClose: () => void;
}) {
  const titleId = useId();
  return (
    <BottomSheet open={request !== null} onClose={onClose} labelledBy={titleId}>
      {/* 닫을 때마다 내용을 내려 체크를 처음 상태로 돌린다. */}
      {request && (
        <Content
          titleId={titleId}
          action={request.action}
          onLeave={request.onLeave}
          resume={request.resume ?? true}
          onClose={onClose}
        />
      )}
    </BottomSheet>
  );
}

function Content({
  titleId,
  action,
  onLeave,
  resume,
  onClose,
}: {
  titleId: string;
  action: PendingAction;
  onLeave?: () => void;
  resume: boolean;
  onClose: () => void;
}) {
  // 첫 로그인인지는 OAuth 전에 알 수 없어 매번 묻는다. 한 번 로그인한 사용자는 시트를 다시 보지 않는다.
  const [adult, setAdult] = useState(false);
  const queryClient = useQueryClient();
  const toast = useToast();

  function remember() {
    const { pathname, search } = window.location;
    // 이어서 하지 않을 때는 예전에 남은 동작도 지운다.
    if (resume) savePending(action, pathname + search);
    else clearPending();
    return { pathname, search };
  }

  function continueWithGoogle() {
    const { pathname, search } = remember();
    onLeave?.();
    window.location.assign(loginUrl(pathname, search));
  }

  async function continueWithDevLogin() {
    const email = window.prompt("개발용 로그인 이메일", "dev@jieuni.local");
    if (!email) return;
    try {
      const me = await devLogin(email);
      // 로그인이 된 뒤에만 남긴다. me가 채워지면 LoginProvider가 이어서 한다.
      remember();
      queryClient.setQueryData(["me"], me);
      onClose();
    } catch {
      toast("로그인하지 못했어요. 다시 시도해 주세요");
    }
  }

  return (
    <>
      <h2 id={titleId} className={`display-md ${styles.title}`}>
        {TITLE[action.action]}
      </h2>
      <p className={`body-md ${styles.description}`}>로그인하면 저장한 공간과 팔로우한 사람을 내 공간에서 볼 수 있어요</p>
      <label className={`label-md ${styles.check}`}>
        <input type="checkbox" checked={adult} onChange={(e) => setAdult(e.target.checked)} />
        만 14세 이상입니다
      </label>
      <button type="button" className={`label-md ${styles.google}`} disabled={!adult} onClick={continueWithGoogle}>
        Google로 계속하기
      </button>
      {DEV_LOGIN && (
        <button type="button" className={`label-sm ${styles.dev}`} disabled={!adult} onClick={continueWithDevLogin}>
          개발용 로그인
        </button>
      )}
      <p className={`body-sm ${styles.terms}`}>
        {/* 새 창으로 열어 시트와 누르던 동작이 그대로 남게 한다. */}
        계속하면{" "}
        <a href="/terms" target="_blank" rel="noopener noreferrer">
          이용약관
        </a>
        과{" "}
        <a href="/privacy" target="_blank" rel="noopener noreferrer">
          개인정보처리방침
        </a>
        에 동의하는 것으로 봐요
      </p>
    </>
  );
}
