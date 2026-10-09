"use client";

import { useQueryClient } from "@tanstack/react-query";
import { useId, useState, type FormEvent } from "react";
import { useLoginSheet } from "@/features/auth/components/LoginProvider";
import { useRequireLogin } from "@/features/auth/useRequireLogin";
import { ApiError, isUnauthorized } from "@/shared/api/client";
import type { CreditRole } from "@/shared/api/types";
import { ROLE_LABEL } from "@/shared/lib/labels";
import { track } from "@/shared/lib/track";
import { BottomSheet } from "@/shared/ui/BottomSheet";
import form from "@/shared/ui/Form.module.css";
import { useToast } from "@/shared/ui/Toast";
import { optional, suggestCredit } from "../api";
import styles from "./Suggest.module.css";

// 상세의 "크레딧을 알려주세요". 비로그인이면 로그인 시트, 로그인했으면 크레딧 시트를 연다.
export function CreditSuggestButton({ placeId, className }: { placeId: number; className: string }) {
  const [open, setOpen] = useState(false);
  const requireLogin = useRequireLogin();
  const titleId = useId();

  return (
    <>
      <button
        type="button"
        className={className}
        onClick={() => requireLogin({ action: { action: "suggest" } }, async () => setOpen(true))}
      >
        이 공간을 만드셨나요? 크레딧을 알려주세요 →
      </button>
      <BottomSheet open={open} onClose={() => setOpen(false)} labelledBy={titleId}>
        {/* 닫을 때마다 내용을 내려 입력을 처음 상태로 돌린다. */}
        {open && <CreditForm titleId={titleId} placeId={placeId} onDone={() => setOpen(false)} />}
      </BottomSheet>
    </>
  );
}

function CreditForm({ titleId, placeId, onDone }: { titleId: string; placeId: number; onDone: () => void }) {
  const [role, setRole] = useState<CreditRole | "">("");
  const [makerName, setMakerName] = useState("");
  const [sourceUrl, setSourceUrl] = useState("");
  const [isSelf, setIsSelf] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const [sending, setSending] = useState(false);
  const { openLogin } = useLoginSheet();
  const queryClient = useQueryClient();
  const toast = useToast();

  async function submit(e: FormEvent) {
    e.preventDefault();
    setSending(true);
    setErrors({});
    setNotice(null);
    try {
      await suggestCredit({ placeId, role: role || null, makerName: makerName.trim(), sourceUrl: optional(sourceUrl), isSelf });
      track("suggestion_submit", { type: "credit" });
      queryClient.invalidateQueries({ queryKey: ["me", "suggestions"] });
      onDone();
      toast("고마워요. 확인하고 반영할게요");
    } catch (err) {
      if (isUnauthorized(err)) {
        // 세션이 끝났다. 이 시트는 그대로 두고 로그인 시트를 위에 연다.
        openLogin({ action: { action: "suggest" } });
      } else if (err instanceof ApiError && err.fieldErrors) {
        setErrors(err.fieldErrors);
      } else if (err instanceof ApiError && (err.status === 429 || err.status === 404)) {
        setNotice(err.message);
      } else {
        toast("보내지 못했어요. 다시 시도해 주세요");
      }
    } finally {
      setSending(false);
    }
  }

  const errorText = (field: string) =>
    errors[field] && (
      <p id={`${titleId}-${field}-error`} className={`body-sm ${form.error}`}>
        {errors[field]}
      </p>
    );

  return (
    <>
      <h2 id={titleId} className={`display-md ${styles.sheetTitle}`}>
        크레딧 알려주기
      </h2>
      <form className={styles.sheetForm} onSubmit={submit} noValidate>
        <div className={form.field}>
          <label htmlFor={`${titleId}-role`} className={`label-sm ${form.label}`}>
            역할
          </label>
          <select
            id={`${titleId}-role`}
            className={`body-md ${form.select}`}
            value={role}
            onChange={(e) => setRole(e.target.value as CreditRole | "")}
            aria-invalid={Boolean(errors.role)}
          >
            <option value="">골라 주세요</option>
            {(Object.keys(ROLE_LABEL) as CreditRole[]).map((value) => (
              <option key={value} value={value}>
                {ROLE_LABEL[value]}
              </option>
            ))}
          </select>
          {errorText("role")}
        </div>
        <div className={form.field}>
          <label htmlFor={`${titleId}-name`} className={`label-sm ${form.label}`}>
            이름
          </label>
          <input
            id={`${titleId}-name`}
            className={`body-md ${form.input}`}
            value={makerName}
            maxLength={100}
            placeholder="사람 또는 스튜디오 이름"
            onChange={(e) => setMakerName(e.target.value)}
            aria-invalid={Boolean(errors.makerName)}
          />
          {errorText("makerName")}
        </div>
        <div className={form.field}>
          <label htmlFor={`${titleId}-source`} className={`label-sm ${form.label}`}>
            출처 링크 (선택)
          </label>
          <input
            id={`${titleId}-source`}
            type="url"
            className={`body-md ${form.input}`}
            value={sourceUrl}
            maxLength={500}
            placeholder="https://"
            onChange={(e) => setSourceUrl(e.target.value)}
            aria-invalid={Boolean(errors.sourceUrl)}
          />
          {errorText("sourceUrl")}
        </div>
        <label className={`label-md ${form.check}`}>
          <input type="checkbox" checked={isSelf} onChange={(e) => setIsSelf(e.target.checked)} />
          제가 만든 사람이에요
        </label>
        {notice && (
          <p className={`body-md ${styles.notice}`} role="alert">
            {notice}
          </p>
        )}
        <button type="submit" className={`label-md ${form.primary}`} disabled={sending}>
          보내기
        </button>
      </form>
    </>
  );
}
