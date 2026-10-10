"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { ACCESS_LABEL, KIND_LABEL } from "@/shared/lib/labels";
import form from "@/shared/ui/Form.module.css";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { STATUS_LABEL, updatedAtLabel } from "../labels";
import { draftSchema, toFieldErrors, toFormValues, type PlaceFormValues } from "../placeSchema";
import { useAdminPlace, useEditAdminPlace } from "../queries";
import type { AdminPlace, Senses } from "../types";
import { AdminHead } from "./AdminHead";
import { CreditsBlock, PhotosBlock, SourcesBlock, StatusBlock } from "./PlaceAttached";
import { PlaceFormField, type FieldSpec } from "./PlaceFormField";
import admin from "./Admin.module.css";
import styles from "./PlaceForm.module.css";

type SenseKey = keyof Senses<string>;
type FieldKey = Exclude<keyof PlaceFormValues, "senses"> | `senses.${SenseKey}`;
type Field = FieldSpec & { key: FieldKey };

// 한 줄에 칸 하나, 두 개면 나란히.
const BASIC: Field[][] = [
  [{ key: "name", label: "공간 이름", help: "100자까지" }],
  [{ key: "slug", label: "주소(slug)", placeholder: "예: maru-yangpyeong", help: "영문 소문자·숫자·하이픈, 80자까지" }],
  [{ key: "kind", label: "종류", type: "select", options: KIND_LABEL }],
  [{ key: "region", label: "지역", placeholder: "예: 경기 양평군", help: "50자까지" }],
  [{ key: "address", label: "주소", help: "200자까지" }],
  [
    { key: "lat", label: "위도", type: "decimal", placeholder: "37.4919" },
    { key: "lng", label: "경도", type: "decimal", placeholder: "127.4876" },
  ],
];

const VISIT: Field[][] = [
  [{ key: "access", label: "방문 방식", type: "select", options: ACCESS_LABEL }],
  [{ key: "visitNote", label: "방문 전 알아야 할 것 (선택)", help: "100자까지" }],
  [
    {
      key: "visitedByEditorOn",
      label: "머물러 본 달 (선택)",
      type: "month",
      placeholder: "2026-05",
      help: "비워 두면 출처가 1개 이상 필요해요",
    },
  ],
];

const SENSE = (key: SenseKey, label: string): Field => ({ key: `senses.${key}`, label: `${label} (선택)`, help: "60자까지" });

const FEELING: Field[][] = [
  [{ key: "summary", label: "한 줄 소개", help: "60자까지 · 목록과 공유 카드에 쓰여요" }],
  [{ key: "impression", label: "감상", type: "textarea", rows: 6, help: "400자까지" }],
  [SENSE("light", "빛"), SENSE("sound", "소리")],
  [SENSE("scent", "향"), SENSE("touch", "촉감")],
];

const ESSAY: Field[][] = [[{ key: "essay", label: "에세이 (선택)", type: "textarea", rows: 12, help: "마크다운" }]];

function read(values: PlaceFormValues, key: FieldKey): string {
  if (key.startsWith("senses.")) return values.senses[key.slice("senses.".length) as SenseKey];
  return values[key as Exclude<FieldKey, `senses.${SenseKey}`>];
}

function write(values: PlaceFormValues, key: FieldKey, value: string): PlaceFormValues {
  if (key.startsWith("senses.")) {
    return { ...values, senses: { ...values.senses, [key.slice("senses.".length)]: value } };
  }
  return { ...values, [key]: value };
}

function draftErrors(values: PlaceFormValues): Record<string, string> {
  const result = draftSchema.safeParse(values);
  return result.success ? {} : toFieldErrors(result.error.issues);
}

export function AdminPlaceForm({ id }: { id: number }) {
  const { data: place, error, isPending } = useAdminPlace(id);

  if (isPending) return null;
  if (error instanceof ApiError && error.status === 404) {
    return (
      <>
        <AdminHead title="공간" />
        <p className={`body-md ${admin.empty}`}>찾을 수 없는 공간이에요</p>
        <Link href="/admin/places" className={`label-md ${admin.shortcut}`}>
          공간 목록 →
        </Link>
      </>
    );
  }
  if (!place) {
    return (
      <>
        <AdminHead title="공간" />
        <p className={`body-md ${admin.empty}`}>불러오지 못했어요. 다시 시도해 주세요</p>
      </>
    );
  }
  // 입력칸은 처음 받은 값으로 한 번만 채운다. 저장 응답이나 다시 받은 값이 입력 중인 칸을 덮지 않게.
  return <PlaceForm key={place.id} place={place} />;
}

function PlaceForm({ place }: { place: AdminPlace }) {
  const [values, setValues] = useState(() => toFormValues(place));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const edit = useEditAdminPlace(place.id);

  function change(key: FieldKey, value: string) {
    setValues((v) => write(v, key, value));
    if (errors[key]) {
      setErrors((e) => {
        const next = { ...e };
        delete next[key];
        return next;
      });
    }
  }

  // 칸을 떠날 때는 그 칸의 형식만 알린다. 메시지를 지우는 것은 고칠 때(서버가 준 메시지도 남겨 둔다).
  function check(key: FieldKey) {
    const message = draftErrors(values)[key];
    if (message) setErrors((e) => ({ ...e, [key]: message }));
  }

  function save(e: FormEvent) {
    e.preventDefault();
    if (edit.isPending) return;
    const parsed = draftSchema.safeParse(values);
    if (!parsed.success) {
      setErrors(toFieldErrors(parsed.error.issues));
      setNotice("입력한 값을 확인해 주세요");
      return;
    }
    setErrors({});
    setNotice(null);
    edit.mutate(parsed.data, {
      onError: (err) => {
        if (err instanceof ApiError && err.status === 409) {
          // PATCH의 409는 slug 중복과 공개 뒤 slug 변경뿐이다.
          setErrors({ slug: err.message });
          setNotice("입력한 값을 확인해 주세요");
        } else if (err instanceof ApiError && err.status === 400) {
          setErrors(err.fieldErrors ?? {});
          setNotice(err.message);
        } else {
          setNotice("저장하지 못했어요. 다시 시도해 주세요");
        }
      },
    });
  }

  function fields(rows: Field[][]) {
    return rows.map((row) => (
      <div key={row[0].key} className={row.length > 1 ? styles.pair : undefined}>
        {row.map((field) => (
          <PlaceFormField
            key={field.key}
            field={field}
            value={read(values, field.key)}
            error={errors[field.key]}
            onChange={(value) => change(field.key, value)}
            onBlur={() => check(field.key)}
          />
        ))}
      </div>
    ));
  }

  const status = edit.isPending
    ? "저장 중…"
    : (notice ?? (edit.isSuccess ? `저장했어요 · ${updatedAtLabel(place.updatedAt)}` : null));

  return (
    <>
      <AdminHead title={place.name} status={STATUS_LABEL[place.status]} />
      <form className={styles.form} onSubmit={save} noValidate>
        <div className={styles.ai}>
          <button type="button" className={`label-md ${form.secondary}`} disabled>
            AI로 초안 채우기
          </button>
        </div>

        <section className={styles.group}>
          <SectionLabel>기본 정보</SectionLabel>
          {fields(BASIC)}
        </section>
        <section className={styles.group}>
          <SectionLabel>방문 정보</SectionLabel>
          {fields(VISIT)}
        </section>
        <section className={styles.group}>
          <SectionLabel>감상과 감각</SectionLabel>
          {fields(FEELING)}
        </section>
        <CreditsBlock place={place} errors={errors} />
        <SourcesBlock place={place} errors={errors} />
        <PhotosBlock place={place} errors={errors} />
        <section className={styles.group}>
          <SectionLabel>에세이</SectionLabel>
          {fields(ESSAY)}
        </section>
        <StatusBlock place={place} />

        <div className={styles.bar}>
          <p className={`${notice ? "body-sm" : "label-sm"} ${styles.barStatus}`} role="status">
            {status}
          </p>
          <button type="submit" className={`label-md ${form.primary}`} disabled={edit.isPending}>
            임시저장
          </button>
        </div>
      </form>
    </>
  );
}
