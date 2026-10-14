"use client";

import { useEffect, useId, useState, type KeyboardEvent } from "react";
import type { MakerKind } from "@/shared/api/types";
import { MAKER_KIND_LABEL } from "@/shared/lib/labels";
import form from "@/shared/ui/Form.module.css";
import { useCreateAdminMaker, useMakerSearch } from "../queries";
import { FieldLabel } from "./PlaceFormField";
import styles from "./PlaceForm.module.css";

export type PickedMaker = { id: number | null; name: string };

// 이름으로 찾아 고른다. 고르기 전에 글자를 바꾸면 고른 사람이 풀린다(id null).
// allowCreate면 찾는 이름이 없을 때 그 자리에서 종류만 골라 만든다.
export function MakerCombobox({
  id,
  name,
  label,
  value,
  onChange,
  error,
  allowCreate = false,
  excludeId,
  onBlur,
}: {
  id: string;
  name: string;
  label: string;
  value: PickedMaker;
  onChange: (value: PickedMaker) => void;
  error?: string;
  allowCreate?: boolean;
  excludeId?: number;
  onBlur?: () => void;
}) {
  const listId = useId();
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const [query, setQuery] = useState("");
  const [creating, setCreating] = useState(false);
  const [kind, setKind] = useState<MakerKind | "">("");
  const [createError, setCreateError] = useState<string | null>(null);
  const create = useCreateAdminMaker();

  const text = value.name;
  // 칠 때마다 부르지 않게 잠깐 기다린다.
  useEffect(() => {
    const timer = setTimeout(() => setQuery(text.trim()), 200);
    return () => clearTimeout(timer);
  }, [text]);

  const { data, isFetching } = useMakerSearch(query, open);
  const found = (data?.items ?? []).filter((m) => m.id !== excludeId);
  const exact = found.some((m) => m.name === text.trim());
  // 지금 친 이름의 검색 결과를 받은 뒤에만 만들기를 보인다(같은 이름이 있는데 또 만드는 것을 막는다).
  const searched = query === text.trim() && !isFetching;
  const canCreate = allowCreate && searched && text.trim() !== "" && !exact;
  const count = found.length + (canCreate ? 1 : 0);

  function pick(index: number) {
    if (index < found.length) {
      onChange({ id: found[index].id, name: found[index].name });
      setOpen(false);
    } else if (canCreate) {
      setCreating(true);
      setOpen(false);
    }
  }

  function key(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
      e.preventDefault();
      setOpen(true);
      if (count > 0) setActive((a) => (a + (e.key === "ArrowDown" ? 1 : count - 1)) % count);
    } else if (e.key === "Enter" && open && count > 0) {
      // 폼 제출(임시저장) 대신 고른다.
      e.preventDefault();
      pick(active);
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  function make() {
    if (!kind) {
      setCreateError("종류를 골라 주세요");
      return;
    }
    setCreateError(null);
    create.mutate(
      { name: text.trim(), kind, slug: null, region: null, intro: null, website: null, instagram: null, affiliationId: null },
      {
        onSuccess: (maker) => {
          onChange({ id: maker.id, name: maker.name });
          setCreating(false);
          setKind("");
        },
        onError: () => setCreateError("만들지 못했어요. 다시 시도해 주세요"),
      },
    );
  }

  const describedBy = [error && `${id}-error`, `${id}-help`].filter(Boolean).join(" ");

  return (
    <div className={`${form.field} ${styles.combobox}`}>
      <FieldLabel htmlFor={id} label={label} />
      <input
        id={id}
        name={name}
        type="text"
        role="combobox"
        autoComplete="off"
        aria-expanded={open && count > 0}
        aria-controls={listId}
        aria-activedescendant={open && count > 0 ? `${listId}-${active}` : undefined}
        aria-invalid={Boolean(error)}
        aria-describedby={describedBy}
        value={text}
        placeholder="이름으로 찾기"
        className={`body-md ${form.input} ${form.raised}`}
        onChange={(e) => {
          onChange({ id: null, name: e.target.value });
          setOpen(true);
          setActive(0);
          setCreating(false);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => {
          setOpen(false);
          onBlur?.();
        }}
        onKeyDown={key}
      />
      {open && count > 0 && (
        <ul id={listId} role="listbox" className={styles.options}>
          {found.map((maker, i) => (
            <li
              key={maker.id}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === active}
              className={`${styles.option} ${i === active ? styles.optionActive : ""}`}
              // 입력칸의 blur가 먼저 와서 목록이 닫히지 않게 누르는 순간을 막는다.
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => pick(i)}
            >
              <span className="body-md">{maker.name}</span>
              <span className={`label-sm ${styles.sub}`}>
                {[MAKER_KIND_LABEL[maker.kind], maker.region, `공간 ${maker.placeCount}곳`].filter(Boolean).join(" · ")}
              </span>
            </li>
          ))}
          {canCreate && (
            <li
              id={`${listId}-${found.length}`}
              role="option"
              aria-selected={active === found.length}
              className={`${styles.option} ${active === found.length ? styles.optionActive : ""}`}
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => pick(found.length)}
            >
              <span className="body-md">‘{text.trim()}’ 새로 만들기</span>
            </li>
          )}
        </ul>
      )}
      {creating && (
        <div className={styles.createRow}>
          <label htmlFor={`${id}-kind`} className="visually-hidden">
            새 만든 사람의 종류
          </label>
          <select
            id={`${id}-kind`}
            value={kind}
            className={`body-md ${form.select} ${form.raised}`}
            onChange={(e) => setKind(e.target.value as MakerKind | "")}
          >
            <option value="">종류를 골라 주세요</option>
            {Object.entries(MAKER_KIND_LABEL).map(([code, kindLabel]) => (
              <option key={code} value={code}>
                {kindLabel}
              </option>
            ))}
          </select>
          <button type="button" className={`label-md ${form.secondary}`} disabled={create.isPending} onClick={make}>
            만들기
          </button>
          <button type="button" className={`label-sm ${styles.textButton}`} onClick={() => setCreating(false)}>
            취소
          </button>
        </div>
      )}
      <p id={`${id}-help`} className={`label-sm ${form.help}`}>
        {value.id ? "골랐어요" : allowCreate ? "목록에서 고르거나 새로 만들어요" : "목록에서 골라요"}
      </p>
      {(createError || error) && (
        <p id={`${id}-error`} className={`body-sm ${form.error}`}>
          {createError ?? error}
        </p>
      )}
    </div>
  );
}
