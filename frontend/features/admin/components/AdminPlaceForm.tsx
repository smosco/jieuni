"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { ACCESS_LABEL, KIND_LABEL } from "@/shared/lib/labels";
import form from "@/shared/ui/Form.module.css";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { STATUS_LABEL, updatedAtLabel } from "../labels";
import { checkLists, rowKey, toRows, type ListRows } from "../listSchema";
import { checkPhotos, toPhotoRows, type PhotoRow } from "../photoSchema";
import { PhotoUploadError, toWebp, uploadConverted } from "../photoUpload";
import {
  draftSchema,
  isDirty,
  publishErrors,
  toFieldErrors,
  toFormValues,
  type PlaceFormValues,
} from "../placeSchema";
import { useAdminPlace, useChangePlaceStatus, useSavePlace } from "../queries";
import type { AdminPlace, AdminPlaceStatus, Senses } from "../types";
import { AdminHead } from "./AdminHead";
import { useLeaveGuard } from "./LeaveGuard";
import { PhotosEditor } from "./PhotosEditor";
import { StatusBlock } from "./PlaceAttached";
import { PlaceFormField, type FieldSpec } from "./PlaceFormField";
import { CreditsEditor, EmbedsEditor, SourcesEditor } from "./PlaceListEditors";
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

type Transition = { to: AdminPlaceStatus; label: string; done: string; confirm?: string };

const CLOSE: Transition = {
  to: "closed",
  label: "폐업",
  done: "폐업으로 바꿨어요",
  confirm: "폐업으로 바꾸면 되돌릴 수 없어요. 바꿀까요?",
};

// 폼에서 누를 수 있는 상태 전환. 서버 전환표의 부분집합이다(제보 → 작성 중·반려는 제보 검토 화면에서).
const TRANSITIONS: Partial<Record<AdminPlaceStatus, Transition[]>> = {
  draft: [{ to: "published", label: "공개", done: "공개했어요", confirm: "공개하면 주소(slug)를 바꿀 수 없어요. 공개할까요?" }],
  published: [{ to: "paused", label: "휴업", done: "휴업으로 바꿨어요" }, CLOSE],
  paused: [{ to: "published", label: "다시 공개", done: "다시 공개했어요" }, CLOSE],
};

// 공개 페이지가 있는 상태. 고칠 때도 서버가 공개 검증을 한다.
const HAS_PAGE: AdminPlaceStatus[] = ["published", "paused", "closed"];

// 공개 검증 안내를 칸에 보일 상태. 제보·반려된 공간은 아직 공개와 상관없다.
const CHECKS_PUBLISH: AdminPlaceStatus[] = ["draft", "published", "paused", "closed"];

function PlaceForm({ place }: { place: AdminPlace }) {
  const [values, setValues] = useState(() => toFormValues(place));
  const [lists, setLists] = useState(() => toRows(place));
  const [photos, setPhotos] = useState(() => toPhotoRows(place.photos));
  // 큰 사진 여러 장을 한꺼번에 바꾸면 메모리를 많이 쓰므로 한 장씩 차례로 올린다.
  const uploads = useRef(Promise.resolve());
  // 방금 바꾼 사진의 미리보기 blob URL(줄 key → URL). 다시 올리거나 줄을 빼거나 폼을 떠날 때 해제한다.
  const previews = useRef(new Map<string, string>());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);
  const edit = useSavePlace(place.id);
  const transition = useChangePlaceStatus(place.id);

  // 저장한 값은 쿼리 캐시의 place다(저장·전환 응답으로 바뀐다). 입력을 같은 모양으로 정규화해 비교한다.
  const scalarDirty = isDirty(values, place);
  const listCheck = checkLists(lists, place);
  const photoCheck = checkPhotos(photos, place.photos);
  const dirty = scalarDirty || Object.values(listCheck.dirty).some(Boolean) || photoCheck.dirty;
  useLeaveGuard(dirty);
  const hints = CHECKS_PUBLISH.includes(place.status) ? publishErrors(values, place, { ...lists, photos }) : {};
  const publishable = Object.keys(hints).length === 0;
  const busy = edit.isPending || transition.isPending;
  const transitions = TRANSITIONS[place.status] ?? [];

  function change(key: FieldKey, value: string) {
    setValues((v) => write(v, key, value));
    setNotice(null);
    if (errors[key]) {
      setErrors((e) => {
        const next = { ...e };
        delete next[key];
        return next;
      });
    }
  }

  // 목록을 고치면 그 목록의 줄 메시지를 지운다(번호가 바뀌어 다른 줄을 가리킬 수 있다).
  function changeList<K extends keyof ListRows>(name: K, rows: ListRows[K]) {
    setLists((l) => ({ ...l, [name]: rows }));
    setNotice(null);
    setErrors((e) => Object.fromEntries(Object.entries(e).filter(([key]) => !key.startsWith(`${name}[`))));
  }

  useEffect(() => {
    const urls = previews.current;
    return () => {
      urls.forEach((url) => URL.revokeObjectURL(url));
      urls.clear();
    };
  }, []);

  function setPreview(key: string, blob: Blob): string {
    const old = previews.current.get(key);
    if (old) URL.revokeObjectURL(old);
    const url = URL.createObjectURL(blob);
    previews.current.set(key, url);
    return url;
  }

  function changePhotos(rows: PhotoRow[]) {
    const kept = new Set(rows.map((row) => row.key));
    previews.current.forEach((url, key) => {
      if (!kept.has(key)) {
        URL.revokeObjectURL(url);
        previews.current.delete(key);
      }
    });
    setPhotos(rows);
    setNotice(null);
    setErrors((e) => Object.fromEntries(Object.entries(e).filter(([key]) => !key.startsWith("photos"))));
  }

  // 파일을 WebP 두 크기로 바꿔 저장소에 올린다. 그동안 줄은 상태만 바뀌고, 설명·출처는 계속 입력할 수 있다.
  function upload(key: string, file: File) {
    const set = (patch: Partial<PhotoRow>) =>
      setPhotos((rows) => rows.map((row) => (row.key === key ? { ...row, ...patch } : row)));
    set({ upload: "converting", uploadError: null });
    uploads.current = uploads.current.then(async () => {
      try {
        const converted = await toWebp(file);
        set({
          upload: "uploading",
          width: converted.width,
          height: converted.height,
          preview: setPreview(key, converted.w800),
        });
        set({ upload: "done", objectKey: await uploadConverted(place.id, converted), file: null });
      } catch (err) {
        set({ upload: "failed", uploadError: err instanceof PhotoUploadError ? err.message : "올리지 못했어요" });
      }
    });
  }

  function addFiles(files: File[]) {
    const rows: PhotoRow[] = files.map((file) => ({
      key: rowKey(),
      objectKey: null,
      width: 0,
      height: 0,
      preview: "",
      upload: "converting",
      uploadError: null,
      file,
      alt: "",
      credit: "",
      license: "",
      sourceUrl: "",
    }));
    setPhotos((current) => [...current, ...rows]);
    setNotice(null);
    rows.forEach((row) => upload(row.key, row.file as File));
  }

  function retry(key: string) {
    const file = photos.find((row) => row.key === key)?.file;
    if (file) upload(key, file);
  }

  // 칸을 떠날 때는 그 칸의 형식만 알린다. 메시지를 지우는 것은 고칠 때(서버가 준 메시지도 남겨 둔다).
  function check(key: FieldKey) {
    const message = draftErrors(values)[key];
    if (message) setErrors((e) => ({ ...e, [key]: message }));
  }

  function fail(err: unknown, fallback: string) {
    if (err instanceof ApiError && err.status === 400) {
      setErrors(err.fieldErrors ?? {});
      setNotice(err.message);
    } else if (err instanceof ApiError && err.status === 409) {
      setNotice(err.message);
    } else {
      setNotice(fallback);
    }
  }

  function save(e: FormEvent) {
    e.preventDefault();
    if (busy) return;
    if (photoCheck.busy || photoCheck.failed) {
      setNotice(photoCheck.busy ? "사진을 올리는 중이에요. 끝난 뒤 저장해 주세요" : "올리지 못한 사진이 있어요. 다시 올리거나 빼 주세요");
      return;
    }
    // 사진 삭제는 한 장씩 따로 커밋되고 파일도 지워진다. 공개 페이지가 있는 공간에서 사진·임베드가 모두 없어지는 저장은
    // 서버가 마지막 삭제에서야 거절해 앞의 삭제만 남으므로 보내지 않는다(서버도 같은 이유로 이 저장 전체를 받아들이지 않는다).
    if (HAS_PAGE.includes(place.status) && hints.photos) {
      setErrors({ photos: hints.photos });
      setNotice("공개된 공간이라 이 값은 비울 수 없어요");
      return;
    }
    const parsed = draftSchema.safeParse(values);
    const formatErrors = {
      ...(parsed.success ? {} : toFieldErrors(parsed.error.issues)),
      ...listCheck.errors,
      ...photoCheck.errors,
    };
    if (!parsed.success || Object.keys(formatErrors).length > 0) {
      setErrors(formatErrors);
      setNotice("입력한 값을 확인해 주세요");
      return;
    }
    setErrors({});
    setNotice(null);
    setDone(null);
    edit.mutate(
      { edit: scalarDirty ? parsed.data : undefined, lists: listCheck.changes, photos: photoCheck.changes },
      {
        onSuccess: (saved) => setDone(saved ? `저장했어요 · ${updatedAtLabel(saved.updatedAt)}` : "바뀐 것이 없어요"),
        onError: (err) => {
          if (err instanceof ApiError && err.status === 409 && !err.fieldErrors) {
            // 칸 정보가 없는 409는 PATCH의 slug 중복과 공개 뒤 slug 변경이다(사진 중복 등록은 그 줄 키를 단다).
            setErrors({ slug: err.message });
            setNotice("입력한 값을 확인해 주세요");
          } else {
            fail(err, "저장하지 못했어요. 다시 시도해 주세요");
          }
        },
      },
    );
  }

  // 서버는 저장된 값으로 전환을 판단하므로, 저장하지 않은 변경이 있으면 버튼을 끈다.
  function changeStatus(t: Transition) {
    if (busy || dirty) return;
    if (t.confirm && !(t.to === "published" && place.publishedAt) && !window.confirm(t.confirm)) return;
    setErrors({});
    setNotice(null);
    setDone(null);
    transition.mutate(t.to, {
      onSuccess: () => setDone(t.done),
      onError: (err) => fail(err, "바꾸지 못했어요. 다시 시도해 주세요"),
    });
  }

  function fields(rows: Field[][]) {
    return rows.map((row) => (
      <div key={row[0].key} className={row.length > 1 ? styles.pair : undefined}>
        {row.map((spec) => {
          const field =
            spec.key === "slug" && place.slugLocked
              ? { ...spec, readOnly: true, help: "공개한 뒤에는 바꿀 수 없어요" }
              : spec;
          return (
            <PlaceFormField
              key={field.key}
              field={field}
              value={read(values, field.key)}
              error={errors[field.key] ?? hints[field.key]}
              onChange={(value) => change(field.key, value)}
              onBlur={() => check(field.key)}
            />
          );
        })}
      </div>
    ));
  }

  const status = edit.isPending
    ? "저장 중…"
    : transition.isPending
      ? "바꾸는 중…"
      : (notice ??
        (dirty
          ? "저장하지 않은 변경이 있어요"
          : (done ?? (place.status === "draft" && !publishable ? "공개하려면 표시한 칸을 채워 주세요" : null))));
  const attachedErrors = { ...hints, ...errors };
  const publishes = transitions.some((t) => t.to === "published");

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
        <section className={styles.group}>
          <SectionLabel>크레딧</SectionLabel>
          <CreditsEditor rows={lists.credits} onChange={(rows) => changeList("credits", rows)} errors={errors} />
          <ListError message={attachedErrors.credits} />
        </section>
        <section className={styles.group}>
          <SectionLabel>출처와 임베드</SectionLabel>
          <h3 className={`label-sm ${styles.subhead}`}>출처</h3>
          <SourcesEditor rows={lists.sources} onChange={(rows) => changeList("sources", rows)} errors={errors} />
          <ListError message={attachedErrors.sources} />
          <h3 className={`label-sm ${styles.subhead}`}>임베드</h3>
          <EmbedsEditor rows={lists.embeds} onChange={(rows) => changeList("embeds", rows)} errors={errors} />
        </section>
        <section className={styles.group}>
          <SectionLabel>사진</SectionLabel>
          <PhotosEditor
            rows={photos}
            savedKeys={new Set(place.photos.map((photo) => photo.objectKey))}
            onChange={changePhotos}
            onFiles={addFiles}
            onRetry={retry}
            errors={attachedErrors}
          />
          <ListError message={attachedErrors.photos} />
        </section>
        <section className={styles.group}>
          <SectionLabel>에세이</SectionLabel>
          {fields(ESSAY)}
        </section>
        <StatusBlock place={place} />

        <div className={styles.bar}>
          <p className={`${notice ? "body-sm" : "label-sm"} ${styles.barStatus}`} role="status">
            {status}
          </p>
          <div className={styles.barActions}>
            {/* 미리보기는 저장된 응답을 그린다. 저장 안 된 변경이 있으면 다른 내용이 보이므로 끈다. */}
            {dirty ? (
              <button type="button" className={`label-md ${form.secondary}`} disabled>
                미리보기
              </button>
            ) : (
              <Link
                href={`/admin/places/${place.id}/preview`}
                target="_blank"
                className={`label-md ${form.secondary} ${admin.linkButton}`}
              >
                미리보기
              </Link>
            )}
            {transitions
              .filter((t) => t.to !== "published")
              .map((t) => (
                <button
                  key={t.to}
                  type="button"
                  className={`label-md ${form.secondary}`}
                  disabled={busy || dirty}
                  onClick={() => changeStatus(t)}
                >
                  {t.label}
                </button>
              ))}
            <button
              type="submit"
              className={`label-md ${publishes ? form.secondary : form.primary}`}
              disabled={busy}
            >
              임시저장
            </button>
            {transitions
              .filter((t) => t.to === "published")
              .map((t) => (
                <button
                  key={t.to}
                  type="button"
                  className={`label-md ${form.primary}`}
                  disabled={busy || dirty || !publishable}
                  onClick={() => changeStatus(t)}
                >
                  {t.label}
                </button>
              ))}
          </div>
        </div>
      </form>
    </>
  );
}

function ListError({ message }: { message?: string }) {
  if (!message) return null;
  return <p className={`body-sm ${styles.error}`}>{message}</p>;
}
