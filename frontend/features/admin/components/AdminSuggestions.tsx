"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent, type ReactNode } from "react";
import { ApiError } from "@/shared/api/client";
import { ROLE_LABEL } from "@/shared/lib/labels";
import form from "@/shared/ui/Form.module.css";
import { TopTabs } from "@/shared/ui/TopTabs";
import { updatedAtLabel } from "../labels";
import {
  useAcceptCreditSuggestion,
  useCreditSuggestions,
  useDeclineCreditSuggestion,
  usePlaceSuggestions,
  useRejectSuggestedPlace,
  useStartSuggestedPlace,
} from "../queries";
import type { CreditSuggestion, PlaceSuggestion, SuggestionType } from "../types";
import { AdminHead } from "./AdminHead";
import { MakerCombobox, type PickedMaker } from "./MakerCombobox";
import { PlaceFormField } from "./PlaceFormField";
import admin from "./Admin.module.css";
import styles from "./PlaceForm.module.css";

// 검토를 기다리는 제보만 먼저 온 순서로. 반려·거절은 되돌릴 수 없어 한 번 묻는다.
export function AdminSuggestions({ type }: { type: SuggestionType }) {
  return (
    <>
      <AdminHead title="제보" />
      <TopTabs
        className={admin.tabs}
        items={[
          { href: "/admin/suggestions", label: "공간 제보", active: type === "places" },
          { href: "/admin/suggestions?type=credits", label: "크레딧 제보", active: type === "credits" },
        ]}
      />
      {type === "places" ? <PlaceSuggestionList /> : <CreditSuggestionList />}
    </>
  );
}

function PlaceSuggestionList() {
  const { data, isError } = usePlaceSuggestions();
  return (
    <ReviewList isError={isError} items={data?.items}>
      {(item) => <PlaceSuggestionRow item={item} />}
    </ReviewList>
  );
}

function CreditSuggestionList() {
  const { data, isError } = useCreditSuggestions();
  return (
    <ReviewList isError={isError} items={data?.items}>
      {(item) => <CreditSuggestionRow item={item} />}
    </ReviewList>
  );
}

function ReviewList<T extends { id: number }>({
  isError,
  items,
  children,
}: {
  isError: boolean;
  items: T[] | undefined;
  children: (item: T) => ReactNode;
}) {
  if (isError) return <p className={`body-md ${admin.empty}`}>불러오지 못했어요. 다시 시도해 주세요</p>;
  if (!items) return null;
  if (items.length === 0) return <p className={`body-md ${admin.empty}`}>검토할 제보가 없어요</p>;
  return <ul className={admin.rows}>{items.map((item) => <li key={item.id}>{children(item)}</li>)}</ul>;
}

function byline(user: { nickname: string } | null, createdAt: string): string {
  return `${user ? user.nickname : "탈퇴한 사용자"} · ${updatedAtLabel(createdAt)}`;
}

function PlaceSuggestionRow({ item }: { item: PlaceSuggestion }) {
  const router = useRouter();
  const start = useStartSuggestedPlace();
  const reject = useRejectSuggestedPlace();
  const [notice, setNotice] = useState<string | null>(null);
  const busy = start.isPending || reject.isPending;

  function onStart() {
    setNotice(null);
    start.mutate(item.id, {
      onSuccess: (place) => router.push(`/admin/places/${place.id}`),
      onError: (err) => setNotice(failure(err)),
    });
  }

  function onReject() {
    if (!window.confirm(`‘${item.name}’ 제보를 반려할까요? 되돌릴 수 없어요`)) return;
    setNotice(null);
    reject.mutate(item.id, { onError: (err) => setNotice(failure(err)) });
  }

  return (
    <article className={admin.review}>
      <div className={admin.rowLeft}>
        <p className={`body-md ${admin.rowName}`}>{item.name}</p>
        <p className={`label-sm ${admin.sub}`}>{[item.region, byline(item.suggestedBy, item.createdAt)].filter(Boolean).join(" · ")}</p>
      </div>
      <dl className={styles.facts}>
        <dt className={`label-sm ${admin.meta}`}>오래 생각나는 이유</dt>
        <dd className="body-md">{item.reason ?? "없음"}</dd>
        <dt className={`label-sm ${admin.meta}`}>참고 링크</dt>
        <dd className="body-md">
          {item.link ? (
            <a href={item.link} target="_blank" rel="noopener noreferrer" className={`hit-area ${styles.link}`}>
              {item.link}
            </a>
          ) : (
            "없음"
          )}
        </dd>
        <dt className={`label-sm ${admin.meta}`}>만든 사람 힌트</dt>
        <dd className="body-md">{item.makerHint ?? "없음"}</dd>
      </dl>
      <div className={admin.slotButtons}>
        <button type="button" className={`label-md ${form.secondary}`} disabled={busy} onClick={onStart}>
          작성 시작
        </button>
        <button type="button" className={`label-sm ${styles.textButton}`} disabled={busy} onClick={onReject}>
          반려
        </button>
        {notice && <p className={`body-sm ${styles.error}`}>{notice}</p>}
      </div>
    </article>
  );
}

function CreditSuggestionRow({ item }: { item: CreditSuggestion }) {
  const decline = useDeclineCreditSuggestion();
  const [accepting, setAccepting] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);

  function onDecline() {
    if (!window.confirm(`‘${item.place.name}’의 ${item.makerName} 제보를 거절할까요? 되돌릴 수 없어요`)) return;
    setNotice(null);
    decline.mutate(item.id, { onError: (err) => setNotice(failure(err)) });
  }

  return (
    <article className={admin.review}>
      <div className={admin.rowLeft}>
        <p className="body-md">
          <Link href={`/admin/places/${item.place.id}`} className={styles.link}>
            {item.place.name}
          </Link>
          {` · ${ROLE_LABEL[item.role]}`}
        </p>
        <p className={`label-sm ${admin.sub}`}>{byline(item.user, item.createdAt)}</p>
      </div>
      <dl className={styles.facts}>
        <dt className={`label-sm ${admin.meta}`}>만든 사람</dt>
        <dd className="body-md">
          {item.makerName}
          {item.isSelf && " (본인이라고 밝혔어요)"}
        </dd>
        <dt className={`label-sm ${admin.meta}`}>출처</dt>
        <dd className="body-md">
          {item.sourceUrl ? (
            <a href={item.sourceUrl} target="_blank" rel="noopener noreferrer" className={`hit-area ${styles.link}`}>
              {item.sourceUrl}
            </a>
          ) : (
            "없음"
          )}
        </dd>
      </dl>
      {accepting ? (
        <AcceptEditor item={item} onClose={() => setAccepting(false)} />
      ) : (
        <div className={admin.slotButtons}>
          <button type="button" className={`label-md ${form.secondary}`} onClick={() => setAccepting(true)}>
            반영
          </button>
          <button type="button" className={`label-sm ${styles.textButton}`} disabled={decline.isPending} onClick={onDecline}>
            거절
          </button>
          {notice && <p className={`body-sm ${styles.error}`}>{notice}</p>}
        </div>
      )}
    </article>
  );
}

// 제보의 이름으로 만든 사람을 찾아 고르거나 새로 만든다. 역할은 제보 그대로, 출처를 비우면 서버가 제보의 출처를 쓴다.
function AcceptEditor({ item, onClose }: { item: CreditSuggestion; onClose: () => void }) {
  const accept = useAcceptCreditSuggestion();
  const [maker, setMaker] = useState<PickedMaker>({ id: null, name: item.makerName });
  const [verified, setVerified] = useState(false);
  const [sourceUrl, setSourceUrl] = useState(item.sourceUrl ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);

  function save(e: FormEvent) {
    e.preventDefault();
    setNotice(null);
    if (maker.id === null) {
      setErrors({ makerId: "목록에서 골라 주세요" });
      return;
    }
    setErrors({});
    accept.mutate(
      { id: item.id, body: { makerId: maker.id, verified, sourceUrl: sourceUrl.trim() || null } },
      {
        onError: (err) => {
          // 요청 칸(makerId·sourceUrl)은 그 칸에, 크레딧 목록 전체의 규칙(공개 검증 등)은 아래 문구로.
          const fields = err instanceof ApiError ? (err.fieldErrors ?? {}) : {};
          const { makerId, sourceUrl: source, ...rest } = fields;
          setErrors({ ...(makerId && { makerId }), ...(source && { sourceUrl: source }) });
          setNotice([failure(err), ...Object.values(rest)].join(" · "));
        },
      },
    );
  }

  return (
    <form className={admin.slotEditor} onSubmit={save} noValidate>
      <MakerCombobox
        id={`accept-${item.id}-maker`}
        name="makerId"
        label="만든 사람"
        value={maker}
        onChange={setMaker}
        error={errors.makerId}
        allowCreate
      />
      <label className={`body-md ${form.check}`}>
        <input type="checkbox" checked={verified} onChange={(e) => setVerified(e.target.checked)} />
        출처로 확인했어요
      </label>
      <PlaceFormField
        field={{
          key: `accept-${item.id}-source`,
          label: "출처 링크",
          placeholder: "https://",
          help: item.sourceUrl ? "비우면 제보에 적힌 출처를 써요" : "만든 사람을 확인한 곳",
        }}
        value={sourceUrl}
        error={errors.sourceUrl}
        onChange={setSourceUrl}
      />
      <div className={admin.slotButtons}>
        <button type="submit" className={`label-md ${form.primary}`} disabled={accept.isPending}>
          {accept.isPending ? "반영 중…" : "크레딧에 반영"}
        </button>
        <button type="button" className={`label-sm ${styles.textButton}`} onClick={onClose}>
          취소
        </button>
        {notice && <p className={`body-sm ${styles.error}`}>{notice}</p>}
      </div>
    </form>
  );
}

function failure(err: unknown): string {
  return err instanceof ApiError && err.status < 500 ? err.message : "처리하지 못했어요. 다시 시도해 주세요";
}
