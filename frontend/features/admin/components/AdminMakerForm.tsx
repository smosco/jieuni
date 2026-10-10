"use client";

import Link from "next/link";
import { useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { MAKER_KIND_LABEL } from "@/shared/lib/labels";
import form from "@/shared/ui/Form.module.css";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { updatedAtLabel } from "../labels";
import { isMakerDirty, makerSchema, toMakerValues, type MakerFormValues } from "../makerSchema";
import { toFieldErrors } from "../placeSchema";
import { useAdminMaker, useEditAdminMaker } from "../queries";
import type { AdminMaker } from "../types";
import { AdminHead } from "./AdminHead";
import { useLeaveGuard } from "./LeaveGuard";
import { MakerCombobox } from "./MakerCombobox";
import { PlaceFormField, type FieldSpec } from "./PlaceFormField";
import admin from "./Admin.module.css";
import styles from "./PlaceForm.module.css";

type TextKey = Exclude<keyof MakerFormValues, "affiliation">;
type Field = FieldSpec & { key: TextKey };

const FIELDS: Field[] = [
  { key: "name", label: "이름", help: "100자까지" },
  { key: "slug", label: "주소(slug) (선택)", placeholder: "예: seojin-yoon", help: "비워 두면 프로필이 없어요. 영문 소문자·숫자·하이픈, 80자까지" },
  { key: "kind", label: "종류", type: "select", options: MAKER_KIND_LABEL },
  { key: "region", label: "지역 (선택)", placeholder: "예: 서울 종로구", help: "50자까지" },
  { key: "intro", label: "소개 (선택)", type: "textarea", rows: 4, help: "200자까지" },
  { key: "website", label: "웹사이트 (선택)", placeholder: "https://" },
  { key: "instagram", label: "인스타그램 아이디 (선택)", placeholder: "예: atelier.on", help: "@ 없이 아이디만" },
];

export function AdminMakerForm({ id }: { id: number }) {
  const { data: maker, error, isPending } = useAdminMaker(id);

  if (isPending) return null;
  if (!maker) {
    const missing = error instanceof ApiError && error.status === 404;
    return (
      <>
        <AdminHead title="만든 사람" />
        <p className={`body-md ${admin.empty}`}>
          {missing ? "찾을 수 없는 만든 사람이에요" : "불러오지 못했어요. 다시 시도해 주세요"}
        </p>
        {missing && (
          <Link href="/admin/makers" className={`label-md ${admin.shortcut}`}>
            만든 사람 목록 →
          </Link>
        )}
      </>
    );
  }
  // 입력칸은 처음 받은 값으로 한 번만 채운다(공간 폼과 같다).
  return <MakerForm key={maker.id} maker={maker} />;
}

function MakerForm({ maker }: { maker: AdminMaker }) {
  const [values, setValues] = useState(() => toMakerValues(maker));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const save = useEditAdminMaker(maker.id);
  const dirty = isMakerDirty(values, maker);
  useLeaveGuard(dirty);

  function clear(key: string) {
    setNotice(null);
    setErrors((e) => {
      const next = { ...e };
      delete next[key];
      return next;
    });
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    if (save.isPending) return;
    const parsed = makerSchema.safeParse(values);
    if (!parsed.success) {
      setErrors(toFieldErrors(parsed.error.issues));
      setNotice("입력한 값을 확인해 주세요");
      return;
    }
    setErrors({});
    setNotice(null);
    setDone(null);
    save.mutate(parsed.data, {
      onSuccess: (saved) => setDone(`저장했어요 · ${updatedAtLabel(saved.updatedAt)}`),
      onError: (err) => {
        if (err instanceof ApiError && err.status === 409) {
          // 만든 사람 PATCH의 409는 slug 잠금과 slug 중복뿐이다.
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

  const status = save.isPending ? "저장 중…" : (notice ?? (dirty ? "저장하지 않은 변경이 있어요" : done));

  return (
    <>
      <AdminHead title={maker.name} status={MAKER_KIND_LABEL[maker.kind]} />
      <form className={styles.form} onSubmit={submit} noValidate>
        <section className={styles.group}>
          <SectionLabel>기본 정보</SectionLabel>
          {FIELDS.map((spec) => {
            const field =
              spec.key === "slug" && maker.slugLocked
                ? { ...spec, label: "주소(slug)", readOnly: true, help: "공개된 공간에 연결된 뒤에는 바꿀 수 없어요" }
                : spec;
            return (
              <PlaceFormField
                key={field.key}
                field={field}
                value={values[field.key]}
                error={errors[field.key]}
                onChange={(value) => {
                  setValues((v) => ({ ...v, [field.key]: value }));
                  clear(field.key);
                }}
              />
            );
          })}
          <MakerCombobox
            id="maker-affiliation"
            name="affiliationId"
            label="소속 스튜디오 (선택)"
            value={values.affiliation}
            onChange={(affiliation) => {
              setValues((v) => ({ ...v, affiliation }));
              clear("affiliationId");
            }}
            error={errors.affiliationId}
            excludeId={maker.id}
          />
        </section>

        <section className={styles.group}>
          <SectionLabel>공개된 공간</SectionLabel>
          {maker.places.length === 0 ? (
            <p className={`body-sm ${styles.none}`}>아직 없어요</p>
          ) : (
            <ul className={styles.lines}>
              {maker.places.map((place) => (
                <li key={place.id} className={styles.line}>
                  <span className="body-md">{place.name}</span>
                  {place.caption && <span className={`label-sm ${styles.sub}`}>{place.caption}</span>}
                </li>
              ))}
            </ul>
          )}
        </section>

        <div className={styles.bar}>
          <p className={`${notice ? "body-sm" : "label-sm"} ${styles.barStatus}`} role="status">
            {status}
          </p>
          <div className={styles.barActions}>
            <button type="submit" className={`label-md ${form.primary}`} disabled={save.isPending}>
              저장
            </button>
          </div>
        </div>
      </form>
    </>
  );
}
