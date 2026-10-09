"use client";

import { useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState, type FormEvent } from "react";
import { useLoginSheet } from "@/features/auth/components/LoginProvider";
import { useMe } from "@/features/me/queries";
import { ApiError, isUnauthorized } from "@/shared/api/client";
import { track } from "@/shared/lib/track";
import form from "@/shared/ui/Form.module.css";
import { useToast } from "@/shared/ui/Toast";
import { optional, suggestPlace } from "../api";
import styles from "./Suggest.module.css";

const EMPTY = { name: "", region: "", reason: "", link: "", makerHint: "" };

type Field = keyof typeof EMPTY;

// 서버 길이 제한과 같다. 넘게는 입력되지 않게 막고, 필수 확인은 서버 메시지로 보인다.
const FIELDS: { name: Field; label: string; maxLength: number; placeholder?: string; multiline?: boolean; help?: string }[] = [
  { name: "name", label: "공간 이름", maxLength: 100 },
  { name: "region", label: "지역", maxLength: 50, placeholder: "예: 서울 성북구" },
  { name: "reason", label: "이 공간이 오래 생각나는 이유", maxLength: 300, multiline: true, help: "300자까지" },
  { name: "link", label: "참고 링크 (선택)", maxLength: 500, placeholder: "https://" },
  { name: "makerHint", label: "만든 사람을 아나요? (선택)", maxLength: 100, placeholder: "건축가, 스튜디오 이름 등" },
];

export function SuggestPlace() {
  const { data: me } = useMe();
  const { openLogin } = useLoginSheet();
  const queryClient = useQueryClient();
  const toast = useToast();
  const [values, setValues] = useState(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<{ message: string; slug?: string } | null>(null);
  const [sending, setSending] = useState(false);
  const [done, setDone] = useState(false);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setSending(true);
    setErrors({});
    setNotice(null);
    try {
      await suggestPlace({
        name: values.name.trim(),
        region: values.region.trim(),
        reason: values.reason.trim(),
        link: optional(values.link),
        makerHint: optional(values.makerHint),
      });
      track("suggestion_submit", { type: "place" });
      queryClient.invalidateQueries({ queryKey: ["me", "suggestions"] });
      setValues(EMPTY);
      setDone(true);
    } catch (err) {
      if (isUnauthorized(err)) {
        // 세션이 끝났다. 입력한 내용이 남도록 화면은 그대로 두고 로그인 시트만 연다.
        openLogin({ action: { action: "suggest" } });
      } else if (err instanceof ApiError && err.status === 409) {
        const slug = err.body?.slug;
        setNotice({ message: err.message, slug: typeof slug === "string" ? slug : undefined });
      } else if (err instanceof ApiError && err.status === 429) {
        setNotice({ message: err.message });
      } else if (err instanceof ApiError && err.fieldErrors) {
        setErrors(err.fieldErrors);
      } else {
        toast("보내지 못했어요. 다시 시도해 주세요");
      }
    } finally {
      setSending(false);
    }
  }

  return (
    <>
      <header className={styles.head}>
        <p className={`label-sm ${styles.eyebrow}`}>SUGGEST</p>
        <h1 className={`display-lg ${styles.title}`}>공간 소개하기</h1>
        <p className={`body-md ${styles.intro}`}>오래 생각나는 공간을 알려 주세요.</p>
      </header>

      {me === null && (
        <div className={styles.body}>
          <button
            type="button"
            className={`label-md ${form.secondary}`}
            onClick={() => openLogin({ action: { action: "suggest" } })}
          >
            로그인하고 소개하기
          </button>
        </div>
      )}

      {me && done && (
        <div className={styles.body}>
          <p className={`body-lg ${styles.thanks}`}>고마워요. 읽어보고 소개되면 알려드릴게요.</p>
          <div className={`label-md ${styles.links}`}>
            <Link href="/me">내 공간으로 →</Link>
            <button type="button" className={styles.textButton} onClick={() => setDone(false)}>
              하나 더 소개하기
            </button>
          </div>
        </div>
      )}

      {me && !done && (
        <form className={styles.body} onSubmit={submit} noValidate>
          {FIELDS.map((field) => {
            const id = `suggest-${field.name}`;
            const error = errors[field.name];
            const describedBy = [error && `${id}-error`, field.help && `${id}-help`].filter(Boolean).join(" ") || undefined;
            const common = {
              id,
              name: field.name,
              value: values[field.name],
              maxLength: field.maxLength,
              placeholder: field.placeholder,
              "aria-invalid": Boolean(error),
              "aria-describedby": describedBy,
              onChange: (e: { target: { value: string } }) => setValues((v) => ({ ...v, [field.name]: e.target.value })),
            };
            return (
              <div key={field.name} className={form.field}>
                <label htmlFor={id} className={`label-sm ${form.label}`}>
                  {field.label}
                </label>
                {field.multiline ? (
                  <textarea {...common} rows={5} className={`body-md ${form.textarea}`} />
                ) : (
                  <input {...common} type={field.name === "link" ? "url" : "text"} className={`body-md ${form.input}`} />
                )}
                {field.help && (
                  <p id={`${id}-help`} className={`label-sm ${form.help}`}>
                    {field.help}
                  </p>
                )}
                {error && (
                  <p id={`${id}-error`} className={`body-sm ${form.error}`}>
                    {error}
                  </p>
                )}
              </div>
            );
          })}

          {notice && (
            <p className={`body-md ${styles.notice}`} role="alert">
              {notice.message}
              {notice.slug && (
                <>
                  {" "}
                  <Link href={`/places/${notice.slug}`} className={styles.noticeLink}>
                    보러 가기 →
                  </Link>
                </>
              )}
            </p>
          )}

          <button type="submit" className={`label-md ${form.primary}`} disabled={sending}>
            보내기
          </button>
        </form>
      )}
    </>
  );
}
