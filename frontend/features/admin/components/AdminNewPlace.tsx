"use client";

import Link from "next/link";
import { useRef, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import form from "@/shared/ui/Form.module.css";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { draftSchema, toFieldErrors } from "../placeSchema";
import { useCreateAdminPlace } from "../queries";
import { AdminHead } from "./AdminHead";
import { fieldId, PlaceFormField } from "./PlaceFormField";
import admin from "./Admin.module.css";
import styles from "./PlaceForm.module.css";

const nameSchema = draftSchema.pick({ name: true });

// 이름만 먼저 여러 개 만들어 두고 나중에 하나씩 채운다. 만들어도 이 화면에 남아 다음 이름을 바로 적는다.
// 방금 만든 목록은 이 화면에만 있다(새로고침하면 비고, 공간 목록의 작성 중에서 찾는다).
export function AdminNewPlace() {
  const create = useCreateAdminPlace();
  const [name, setName] = useState("");
  const [error, setError] = useState<string | undefined>();
  const [notice, setNotice] = useState<string | null>(null);
  const [made, setMade] = useState<{ id: number; name: string }[]>([]);
  const formRef = useRef<HTMLFormElement>(null);

  function submit(e: FormEvent) {
    e.preventDefault();
    if (create.isPending) return;
    const parsed = nameSchema.safeParse({ name });
    if (!parsed.success) {
      setError(toFieldErrors(parsed.error.issues).name);
      return;
    }
    setError(undefined);
    setNotice(null);
    create.mutate(
      { name: parsed.data.name, region: null },
      {
        onSuccess: (place) => {
          setMade((list) => [{ id: place.id, name: place.name }, ...list]);
          setNotice(`만들었어요 · ${place.name}`);
          setName("");
          formRef.current?.querySelector<HTMLInputElement>(`#${fieldId("name")}`)?.focus();
        },
        onError: (err) => {
          if (err instanceof ApiError && err.fieldErrors?.name) setError(err.fieldErrors.name);
          else setNotice("만들지 못했어요. 다시 시도해 주세요");
        },
      },
    );
  }

  return (
    <>
      <AdminHead title="새 공간" />
      <form ref={formRef} className={styles.create} onSubmit={submit} noValidate>
        <PlaceFormField
          field={{ key: "name", label: "공간 이름", placeholder: "이름을 적고 Enter", help: "만들면 이 자리에 남아 다음 이름을 바로 적을 수 있어요" }}
          value={name}
          error={error}
          onChange={(value) => {
            setName(value);
            setError(undefined);
          }}
        />
        <button type="submit" className={`label-md ${form.secondary} ${styles.createButton}`} disabled={create.isPending}>
          만들기
        </button>
      </form>
      {notice && (
        <p className={`body-sm ${styles.made}`} role="status">
          {notice}
        </p>
      )}
      {made.length > 0 && (
        <section className={styles.madeList}>
          <SectionLabel>방금 만든 공간</SectionLabel>
          <ul className={admin.rows}>
            {made.map((place) => (
              <li key={place.id}>
                <Link href={`/admin/places/${place.id}`} className={admin.row}>
                  <span className={styles.madeName}>
                    <span className="body-md">{place.name}</span>
                    <span className={`label-sm ${styles.sub}`}>작성 중 · 이름만</span>
                  </span>
                  <span className={`label-md ${styles.back}`}>채우기 →</span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </>
  );
}
