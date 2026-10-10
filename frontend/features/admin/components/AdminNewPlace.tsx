"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import form from "@/shared/ui/Form.module.css";
import { draftSchema, toFieldErrors } from "../placeSchema";
import { useCreateAdminPlace } from "../queries";
import { AdminHead } from "./AdminHead";
import { PlaceFormField, type FieldSpec } from "./PlaceFormField";
import styles from "./PlaceForm.module.css";

// 만들 때는 이름·지역만 받는다(제보와 같은 모양). 나머지는 만든 뒤 수정 폼에서.
const FIELDS: (FieldSpec & { key: "name" | "region" })[] = [
  { key: "name", label: "공간 이름", help: "100자까지" },
  { key: "region", label: "지역 (선택)", placeholder: "예: 경기 양평군", help: "50자까지" },
];

const createSchema = draftSchema.pick({ name: true, region: true });

export function AdminNewPlace() {
  const router = useRouter();
  const create = useCreateAdminPlace();
  const [values, setValues] = useState({ name: "", region: "" });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (create.isPending) return;
    const parsed = createSchema.safeParse(values);
    if (!parsed.success) {
      setErrors(toFieldErrors(parsed.error.issues));
      return;
    }
    setErrors({});
    setNotice(null);
    create.mutate(parsed.data, {
      // 뒤로 가기로 빈 만들기 화면에 돌아오지 않게 바꿔 넣는다.
      onSuccess: (place) => router.replace(`/admin/places/${place.id}`),
      onError: (err) => {
        if (err instanceof ApiError && err.fieldErrors) setErrors(err.fieldErrors);
        else setNotice("만들지 못했어요. 다시 시도해 주세요");
      },
    });
  }

  return (
    <>
      <AdminHead title="새 공간" />
      <form className={styles.form} onSubmit={submit} noValidate>
        <section className={styles.group}>
          {FIELDS.map((field) => (
            <PlaceFormField
              key={field.key}
              field={field}
              value={values[field.key]}
              error={errors[field.key]}
              onChange={(value) => {
                setValues((v) => ({ ...v, [field.key]: value }));
                setErrors((e) => ({ ...e, [field.key]: "" }));
              }}
            />
          ))}
        </section>
        {notice && (
          <p className={`body-sm ${styles.error}`} role="alert">
            {notice}
          </p>
        )}
        <div>
          <button type="submit" className={`label-md ${form.primary}`} disabled={create.isPending}>
            만들기
          </button>
        </div>
      </form>
    </>
  );
}
