"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import { ACCESS_LABEL, KIND_LABEL } from "@/shared/lib/labels";
import form from "@/shared/ui/Form.module.css";
import { STATUS_LABEL, updatedAtLabel } from "../labels";
import { clearDraft, fromDraft, readDraft, restorable, toDraft, writeDraft } from "../draftStore";
import { checkLists, rowKey, toRows, withoutBlankRows, type ListRows } from "../listSchema";
import { checkPhotos, toPhotoRows, type PhotoRow } from "../photoSchema";
import { PhotoUploadError, toWebp, uploadConverted } from "../photoUpload";
import {
  draftSchema,
  hintText,
  isDirty,
  marksVisible,
  PUBLISH_FIELDS,
  publishErrors,
  publishMissingCount,
  saveLabel,
  toFieldErrors,
  toFormValues,
  type PlaceFormValues,
} from "../placeSchema";
import { useAdminPlace, useChangePlaceStatus, useSavePlace } from "../queries";
import type { AdminPlace, AdminPlaceStatus, Senses } from "../types";
import { AdminHead } from "./AdminHead";
import { useConfirmLeave, useLeaveGuard } from "./LeaveGuard";
import { PhotosEditor } from "./PhotosEditor";
import { StatusBlock } from "./PlaceAttached";
import { fieldId, PlaceFormField, type FieldSpec } from "./PlaceFormField";
import { CreditsEditor, EmbedsEditor, SourcesEditor } from "./PlaceListEditors";
import admin from "./Admin.module.css";
import styles from "./PlaceForm.module.css";

type SenseKey = keyof Senses<string>;
type FieldKey = Exclude<keyof PlaceFormValues, "senses"> | `senses.${SenseKey}`;
type Field = FieldSpec & { key: FieldKey };

// 한 줄에 칸 하나, 두 개면 나란히. * 은 공개 검증 표(PUBLISH_FIELDS)에서 붙인다.
const NAME: Field[][] = [[{ key: "name", label: "공간 이름", help: "100자까지 · 이름만 있어도 저장돼요" }]];

const BASIC: Field[][] = [
  [{ key: "kind", label: "종류", type: "select", options: KIND_LABEL }],
  [{ key: "region", label: "지역", placeholder: "예: 경기 양평군", help: "50자까지" }],
  [{ key: "address", label: "주소", help: "200자까지" }],
  [
    {
      key: "slug",
      label: "주소(slug)",
      placeholder: "예: maru-yangpyeong",
      help: "영문 소문자·숫자·하이픈, 80자까지 · 공개하면 바꿀 수 없어요",
    },
  ],
];

const SENSE = (key: SenseKey, label: string): Field => ({ key: `senses.${key}`, label, help: "60자까지" });

const WRITING: Field[][] = [
  [{ key: "summary", label: "한 줄 소개", help: "60자까지 · 목록과 공유 카드에 쓰여요" }],
  [{ key: "impression", label: "감상", type: "textarea", rows: 6, help: "400자까지" }],
  [SENSE("light", "빛"), SENSE("sound", "소리")],
  [SENSE("scent", "향"), SENSE("touch", "촉감")],
];

const VISIT: Field[][] = [
  [{ key: "access", label: "방문 방식", type: "select", options: ACCESS_LABEL }],
  [{ key: "visitNote", label: "방문 전 알아야 할 것", help: "100자까지" }],
  [
    {
      key: "visitedByEditorOn",
      label: "머물러 본 달",
      type: "month",
      placeholder: "2026-05",
      help: "머물러 본 적이 없으면 비우고 더 적기에 출처를 넣어요",
    },
  ],
];

const ESSAY: Field[][] = [[{ key: "essay", label: "에세이", type: "textarea", rows: 12, help: "마크다운" }]];

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

const PUBLISH_CONFIRM = "공개하면 주소(slug)를 바꿀 수 없어요. 공개할까요?";

const CLOSE: Transition = {
  to: "closed",
  label: "폐업",
  done: "폐업으로 바꿨어요",
  confirm: "폐업으로 바꾸면 되돌릴 수 없어요. 바꿀까요?",
};

// ⋯ 메뉴의 상태 전환. 서버 전환표의 부분집합이다(작성 중 → 공개는 큰 버튼, 제보 → 작성 중·반려는 제보 검토 화면에서).
const TRANSITIONS: Partial<Record<AdminPlaceStatus, Transition[]>> = {
  published: [{ to: "paused", label: "휴업", done: "휴업으로 바꿨어요" }, CLOSE],
  paused: [{ to: "published", label: "다시 공개", done: "다시 공개했어요" }, CLOSE],
};

// 공개 페이지가 있는 상태. 고칠 때도 서버가 공개 검증을 한다.
const HAS_PAGE: AdminPlaceStatus[] = ["published", "paused", "closed"];

// 공개 검증(* 표시·남은 칸)을 보일 상태. 제보·반려된 공간은 아직 공개와 상관없다.
const CHECKS_PUBLISH: AdminPlaceStatus[] = ["draft", "published", "paused", "closed"];

function PlaceForm({ place }: { place: AdminPlace }) {
  const [values, setValues] = useState(() => toFormValues(place));
  const [lists, setLists] = useState(() => toRows(place));
  const [photos, setPhotos] = useState(() => toPhotoRows(place.photos));
  // 큰 사진 여러 장을 한꺼번에 바꾸면 메모리를 많이 쓰므로 한 장씩 차례로 올린다.
  const uploads = useRef(Promise.resolve());
  // 방금 바꾼 사진의 미리보기·확대 blob URL(줄 key → URL들). 다시 올리거나 줄을 빼거나 폼을 떠날 때 해제한다.
  const previews = useRef(new Map<string, string[]>());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);
  const [done, setDone] = useState<string | null>(null);
  // 이번 화면에서 저장을 누르면 빈 * 칸 표시를 켠다.
  const [submitted, setSubmitted] = useState(false);
  const [publishing, setPublishing] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const menu = useRef<HTMLDivElement>(null);
  // 렌더가 끝난 뒤 옮겨 갈 칸들(서버 키). 화면 순서로 가장 앞의 칸에 간다.
  const [focusKeys, setFocusKeys] = useState<string[] | null>(null);
  // AdminShell이 관리자 확인 뒤에만 그리므로 이 폼은 브라우저에서만 렌더된다.
  const [restore, setRestore] = useState(() =>
    restorable(readDraft(place.id), toDraft(toFormValues(place), toRows(place), toPhotoRows(place.photos))),
  );
  const edit = useSavePlace(place.id);
  const transition = useChangePlaceStatus(place.id);
  const confirmLeave = useConfirmLeave();

  // 저장한 값은 쿼리 캐시의 place다(저장·전환 응답으로 바뀐다). 입력을 같은 모양으로 정규화해 비교한다.
  const scalarDirty = isDirty(values, place);
  const listCheck = checkLists(lists, place);
  const photoCheck = checkPhotos(photos, place.photos);
  const dirty = scalarDirty || Object.values(listCheck.dirty).some(Boolean) || photoCheck.dirty;
  useLeaveGuard(dirty);
  const checksPublish = CHECKS_PUBLISH.includes(place.status);
  // 빈 목록 줄은 저장 때 빠지므로 공개 검증에서도 뺀다.
  const kept = withoutBlankRows(lists);
  const hints = checksPublish ? publishErrors(values, place, { ...kept, photos }) : {};
  const missing = publishMissingCount(hints);
  const shownHints: Record<string, string> =
    checksPublish && marksVisible(place, submitted)
      ? Object.fromEntries(Object.entries(hints).map(([key, message]) => [key, hintText(key, message)]))
      : {};
  const busy = edit.isPending || transition.isPending;
  const label = checksPublish ? saveLabel(place.status, missing) : "저장";
  const transitions = TRANSITIONS[place.status] ?? [];

  // 묻는 동안에는 보관본을 덮지 않는다. 저장된 값과 같아지면 지운다.
  useEffect(() => {
    if (restore) return;
    if (dirty) writeDraft(place.id, toDraft(values, lists, photos));
    else clearDraft(place.id);
  }, [restore, dirty, values, lists, photos, place.id]);

  useEffect(() => {
    if (!focusKeys) return;
    const targets = focusKeys
      .map((key) => document.getElementById(fieldId(key)))
      .filter((el): el is HTMLElement => el !== null)
      .sort((a, b) => (a.compareDocumentPosition(b) & Node.DOCUMENT_POSITION_FOLLOWING ? -1 : 1));
    if (targets[0]) {
      targets[0].scrollIntoView({ block: "center" });
      targets[0].focus({ preventScroll: true });
    }
  }, [focusKeys]);

  // ⋯ 메뉴는 바깥을 누르거나 Esc로 닫는다.
  useEffect(() => {
    if (!menuOpen) return;
    const close = (e: PointerEvent) => {
      if (!menu.current?.contains(e.target as Node)) setMenuOpen(false);
    };
    const escape = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    document.addEventListener("pointerdown", close);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("pointerdown", close);
      document.removeEventListener("keydown", escape);
    };
  }, [menuOpen]);

  useEffect(() => {
    const urls = previews.current;
    return () => {
      urls.forEach((list) => list.forEach((url) => URL.revokeObjectURL(url)));
      urls.clear();
    };
  }, []);

  // 불러올지 묻는 동안 새로 입력하면 보관본을 버린 것으로 본다. 그래야 지금 입력이 다시 보관된다.
  function edited() {
    if (restore) dropDraft();
    setNotice(null);
    setDone(null);
  }

  function change(key: FieldKey, value: string) {
    setValues((v) => write(v, key, value));
    edited();
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
    edited();
    setErrors((e) => Object.fromEntries(Object.entries(e).filter(([key]) => !key.startsWith(`${name}[`))));
  }

  function setPreviews(key: string, blobs: Blob[]): string[] {
    previews.current.get(key)?.forEach((url) => URL.revokeObjectURL(url));
    const urls = blobs.map((blob) => URL.createObjectURL(blob));
    previews.current.set(key, urls);
    return urls;
  }

  function changePhotos(rows: PhotoRow[]) {
    const keep = new Set(rows.map((row) => row.key));
    previews.current.forEach((urls, key) => {
      if (!keep.has(key)) {
        urls.forEach((url) => URL.revokeObjectURL(url));
        previews.current.delete(key);
      }
    });
    setPhotos(rows);
    edited();
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
        const [preview, full] = setPreviews(key, [converted.w800, converted.w1600]);
        set({ upload: "uploading", width: converted.width, height: converted.height, preview, full });
        set({ upload: "done", objectKey: await uploadConverted(place.id, converted), file: null });
      } catch (err) {
        set({ upload: "failed", uploadError: err instanceof PhotoUploadError ? err.message : "올리지 못했어요" });
      }
    });
  }

  // 사진 종류는 가장 흔한 "직접 찍음"으로 시작한다.
  function addFiles(files: File[]) {
    const rows: PhotoRow[] = files.map((file) => ({
      key: rowKey(),
      objectKey: null,
      width: 0,
      height: 0,
      preview: "",
      full: "",
      upload: "converting",
      uploadError: null,
      file,
      alt: "",
      credit: "",
      license: "own",
      sourceUrl: "",
    }));
    setPhotos((current) => [...current, ...rows]);
    edited();
    rows.forEach((row) => upload(row.key, row.file as File));
  }

  function retry(key: string) {
    const file = photos.find((row) => row.key === key)?.file;
    if (file) upload(key, file);
  }

  // 칸을 떠날 때는 그 칸의 형식만 알린다. 메시지를 지우는 것은 고칠 때(서버가 준 메시지도 남겨 둔다).
  function check(key: string) {
    const message = { ...draftErrors(values), ...listCheck.errors, ...photoCheck.errors }[key];
    if (message) setErrors((e) => ({ ...e, [key]: message }));
  }

  function fail(err: unknown, fallback: string, prefix = "") {
    if (err instanceof ApiError && err.status === 400) {
      setErrors(err.fieldErrors ?? {});
      setNotice(prefix + err.message);
      if (err.fieldErrors) setFocusKeys(Object.keys(err.fieldErrors));
    } else if (err instanceof ApiError && err.status === 409) {
      setNotice(prefix + err.message);
    } else {
      setNotice(prefix + fallback);
    }
  }

  function stop(formatErrors: Record<string, string>, message: string) {
    setErrors(formatErrors);
    setNotice(message);
    setFocusKeys(Object.keys(formatErrors));
  }

  // 큰 버튼 하나: 바뀐 것을 저장하고, "저장하고 공개"면 이어서 공개한다.
  // 저장한 뒤 공개를 취소하거나 공개가 실패하면 저장된 채로 두고 이유를 아래 줄에 보인다.
  async function submit(e: FormEvent) {
    e.preventDefault();
    if (busy) return;
    setSubmitted(true);
    setDone(null);
    setMenuOpen(false);
    if (photoCheck.busy || photoCheck.failed) {
      setNotice(photoCheck.busy ? "사진을 올리는 중이에요. 끝난 뒤 저장해 주세요" : "올리지 못한 사진이 있어요. 다시 올리거나 빼 주세요");
      return;
    }
    // 사진 삭제는 한 장씩 따로 커밋되고 파일도 지워진다. 공개 페이지가 있는 공간에서 사진·임베드가 모두 없어지는 저장은
    // 서버가 마지막 삭제에서야 거절해 앞의 삭제만 남으므로 보내지 않는다(서버도 같은 이유로 이 저장 전체를 받아들이지 않는다).
    if (HAS_PAGE.includes(place.status) && hints.photos) {
      stop({ photos: hints.photos }, "공개된 공간이라 이 값은 비울 수 없어요");
      return;
    }
    const parsed = draftSchema.safeParse(values);
    const formatErrors = {
      ...(parsed.success ? {} : toFieldErrors(parsed.error.issues)),
      ...listCheck.errors,
      ...photoCheck.errors,
    };
    if (!parsed.success || Object.keys(formatErrors).length > 0) {
      stop(formatErrors, "입력한 값을 확인해 주세요");
      return;
    }
    const andPublish = label === "저장하고 공개";
    setErrors({});
    setNotice(null);
    let saved = "";
    if (dirty) {
      try {
        const result = await edit.mutateAsync({
          edit: scalarDirty ? parsed.data : undefined,
          lists: listCheck.changes,
          photos: photoCheck.changes,
        });
        saved = result ? `저장했어요 · ${updatedAtLabel(result.updatedAt)}` : "바뀐 것이 없어요";
      } catch (err) {
        if (err instanceof ApiError && err.status === 409 && !err.fieldErrors) {
          // 칸 정보가 없는 409는 PATCH의 slug 중복과 공개 뒤 slug 변경이다(사진 중복 등록은 그 줄 키를 단다).
          stop({ slug: err.message }, "입력한 값을 확인해 주세요");
        } else {
          fail(err, "저장하지 못했어요. 다시 시도해 주세요");
        }
        return;
      }
    }
    if (!andPublish) {
      setDone(saved || "바뀐 것이 없어요");
      return;
    }
    const prefix = saved ? "저장했어요. " : "";
    if (!window.confirm(PUBLISH_CONFIRM)) {
      setNotice(`${prefix}공개는 하지 않았어요`);
      return;
    }
    setPublishing(true);
    try {
      await transition.mutateAsync("published");
      setDone("공개했어요");
    } catch (err) {
      fail(err, "다시 시도해 주세요", `${prefix}공개하지 못했어요 · `);
    } finally {
      setPublishing(false);
    }
  }

  // 서버는 저장된 값으로 전환을 판단하므로, 저장하지 않은 변경이 있으면 메뉴 항목을 끈다.
  function changeStatus(t: Transition) {
    setMenuOpen(false);
    if (busy || dirty) return;
    if (t.confirm && !window.confirm(t.confirm)) return;
    setErrors({});
    setNotice(null);
    setDone(null);
    transition.mutate(t.to, {
      onSuccess: () => setDone(t.done),
      onError: (err) => fail(err, "바꾸지 못했어요. 다시 시도해 주세요"),
    });
  }

  function loadDraft() {
    if (!restore) return;
    const loaded = fromDraft(restore, place.photos);
    setValues(loaded.values);
    setLists(loaded.lists);
    setPhotos(loaded.photos);
    setErrors({});
    setRestore(null);
  }

  function dropDraft() {
    clearDraft(place.id);
    setRestore(null);
  }

  // 공개에 필요한 칸이 비면 칸 바로 아래 안내. 형식 오류가 있으면 그쪽이 먼저다.
  // 머물러 본 달 칸에는 "머물러 본 달 또는 출처" 규칙(sources)을 단다.
  function fieldError(key: FieldKey): string | undefined {
    if (errors[key]) return errors[key];
    if (key === "visitedByEditorOn") return shownHints.sources && "공개하려면 머물러 본 달이나 출처가 필요해요";
    return shownHints[key];
  }

  function fields(rows: Field[][]) {
    return rows.map((row) => (
      <div key={row[0].key} className={row.length > 1 ? styles.pair : undefined}>
        {row.map((spec) => {
          const required =
            checksPublish &&
            (spec.key === "visitedByEditorOn" ? kept.sources.length === 0 : PUBLISH_FIELDS.has(spec.key));
          const field =
            spec.key === "slug" && place.slugLocked
              ? { ...spec, required, readOnly: true, help: "공개한 뒤에는 바꿀 수 없어요" }
              : { ...spec, required };
          return (
            <PlaceFormField
              key={field.key}
              field={field}
              value={read(values, field.key)}
              error={fieldError(field.key)}
              onChange={(value) => change(field.key, value)}
              onBlur={() => check(field.key)}
            />
          );
        })}
      </div>
    ));
  }

  // 아래 줄 왼쪽: 진행 중 → 실패·안내 → 방금 한 일 → 공개까지 남은 칸.
  const remaining =
    place.status === "draft"
      ? missing > 0
        ? `공개까지 ${missing}칸 남았어요`
        : "공개할 수 있어요"
      : (place.status === "published" || place.status === "paused") && missing > 0
        ? "공개 중이라 * 칸은 비울 수 없어요"
        : null;
  const left = edit.isPending
    ? "저장 중…"
    : publishing
      ? "공개하는 중…"
      : transition.isPending
        ? "바꾸는 중…"
        : (notice ?? done ?? remaining);
  const attachedErrors = { ...shownHints, ...errors };

  return (
    <>
      <header className={admin.head}>
        <div className={admin.headText}>
          <p className={`label-sm ${admin.eyebrow} ${styles.crumbs}`}>
            <Link
              href="/admin/places"
              className={styles.back}
              onNavigate={(e) => {
                if (!confirmLeave()) e.preventDefault();
              }}
            >
              ← 공간 목록
            </Link>
            <span>ADMIN · 공간</span>
          </p>
          <h1 className="visually-hidden">{place.name}</h1>
        </div>
      </header>
      {restore && (
        <div className={styles.restore} role="status">
          <p className="body-sm">저장 안 한 내용이 있어요. 불러올까요?</p>
          <div className={styles.restoreActions}>
            <button type="button" className={`label-md ${form.secondary}`} onClick={loadDraft}>
              불러오기
            </button>
            <button type="button" className={`label-sm ${styles.textButton}`} onClick={dropDraft}>
              버리기
            </button>
          </div>
        </div>
      )}
      <form className={styles.form} onSubmit={submit} noValidate>
        <section className={styles.section}>
          <SectionHead title="이름" />
          {fields(NAME)}
        </section>
        <section id={fieldId("photos")} tabIndex={-1} className={styles.section}>
          <SectionHead
            title="사진"
            required={checksPublish}
            note="공개하려면 사진이나 임베드가 1개 이상 필요해요"
            error={Boolean(attachedErrors.photos)}
          />
          <PhotosEditor
            rows={photos}
            savedKeys={new Set(place.photos.map((photo) => photo.objectKey))}
            onChange={changePhotos}
            onFiles={addFiles}
            onRetry={retry}
            onBlur={check}
            errors={attachedErrors}
          />
        </section>
        <section className={styles.section}>
          <SectionHead title="기본 정보" />
          {fields(BASIC)}
        </section>
        <section className={styles.section}>
          <SectionHead title="글" />
          {fields(WRITING)}
        </section>
        <section id={fieldId("credits")} tabIndex={-1} className={styles.section}>
          <SectionHead
            title="만든 사람"
            required={checksPublish}
            note="출처 링크가 있는 크레딧이 1개 이상 필요해요"
            error={Boolean(attachedErrors.credits)}
          />
          <CreditsEditor
            rows={lists.credits}
            onChange={(rows) => changeList("credits", rows)}
            errors={attachedErrors}
            onBlur={check}
          />
        </section>
        <section className={styles.section}>
          <SectionHead title="방문 정보" />
          {fields(VISIT)}
        </section>
        <details className={styles.section} open>
          <summary className={styles.summary}>
            <SectionHead title="더 적기" note="출처 · 임베드 · 에세이 — 없어도 공개할 수 있어요" />
          </summary>
          <div className={styles.moreBody}>
            <h3 id={fieldId("sources")} tabIndex={-1} className={`label-md ${styles.subhead}`}>
              출처
            </h3>
            <SourcesEditor
              rows={lists.sources}
              onChange={(rows) => changeList("sources", rows)}
              errors={attachedErrors}
              onBlur={check}
            />
            <h3 className={`label-md ${styles.subhead}`}>임베드</h3>
            <EmbedsEditor
              rows={lists.embeds}
              onChange={(rows) => changeList("embeds", rows)}
              errors={attachedErrors}
              onBlur={check}
            />
            {fields(ESSAY)}
          </div>
        </details>
        <div className={styles.section}>
          <StatusBlock place={place} />
        </div>

        <div className={styles.saveBar}>
          <div className={styles.saveState}>
            <span className={`label-sm ${styles.sub}`}>{STATUS_LABEL[place.status]}</span>
            {left && (
              <p className={`body-sm ${notice ? styles.error : styles.saveLeft}`} role="status">
                {left}
              </p>
            )}
          </div>
          <div className={styles.saveActions} ref={menu}>
            <button
              type="button"
              className={styles.moreButton}
              aria-label="더 보기"
              aria-expanded={menuOpen}
              onClick={() => setMenuOpen((open) => !open)}
            >
              ⋯
            </button>
            {menuOpen && (
              <ul className={`body-md ${styles.menu}`}>
                <li>
                  {/* 미리보기는 저장된 응답을 그린다. 저장 안 된 변경이 있으면 다른 내용이 보이므로 끈다. */}
                  {dirty ? (
                    <MenuOff label="미리보기" reason="먼저 저장해 주세요" />
                  ) : (
                    <Link
                      href={`/admin/places/${place.id}/preview`}
                      target="_blank"
                      className={styles.menuItem}
                      onClick={() => setMenuOpen(false)}
                    >
                      미리보기
                    </Link>
                  )}
                </li>
                {place.status === "draft" && (
                  <>
                    <li>
                      <MenuOff label="휴업" reason="공개한 뒤에 쓸 수 있어요" />
                    </li>
                    <li>
                      <MenuOff label="폐업" reason="공개한 뒤에 쓸 수 있어요" />
                    </li>
                  </>
                )}
                {transitions.map((t) => (
                  <li key={t.to}>
                    {dirty || busy ? (
                      <MenuOff label={t.label} reason={dirty ? "먼저 저장해 주세요" : ""} />
                    ) : (
                      <button type="button" className={styles.menuItem} onClick={() => changeStatus(t)}>
                        {t.label}
                      </button>
                    )}
                  </li>
                ))}
              </ul>
            )}
            <button type="submit" className={`label-md ${form.primary}`} disabled={busy}>
              {label}
            </button>
          </div>
        </div>
      </form>
    </>
  );
}

// 구역 제목. required는 칸 밖에서 정해지는 공개 조건(사진·만든 사람)에만 붙인다.
function SectionHead({
  title,
  required,
  note,
  error,
}: {
  title: string;
  required?: boolean;
  note?: string;
  error?: boolean;
}) {
  return (
    <div className={styles.sectionHead}>
      <h2 className={`display-md ${styles.sectionTitle}`}>
        {title}
        {required && <span aria-hidden="true"> *</span>}
      </h2>
      {note && <p className={`body-sm ${error ? styles.error : styles.sectionNote}`}>{note}</p>}
    </div>
  );
}

function MenuOff({ label, reason }: { label: string; reason: string }) {
  return (
    <span className={`${styles.menuItem} ${styles.menuOff}`} aria-disabled="true">
      {label}
      {reason && <span className="label-sm">{reason}</span>}
    </span>
  );
}
