"use client";

import { useEffect, useRef, useState, type DragEvent } from "react";
import { PHOTO_LICENSE_LABEL } from "../labels";
import type { PhotoRow } from "../photoSchema";
import { checkPhotoFile, photoWarning } from "../photoUpload";
import { photoPublishFields } from "../placeSchema";
import { PlaceFormField } from "./PlaceFormField";
import { ReorderList } from "./ReorderList";
import styles from "./PlaceForm.module.css";

const UPLOAD_LABEL: Record<PhotoRow["upload"], string> = {
  converting: "WebP로 바꾸는 중…",
  uploading: "올리는 중…",
  failed: "",
  done: "",
};

function update(rows: PhotoRow[], key: string, patch: Partial<PhotoRow>): PhotoRow[] {
  return rows.map((row) => (row.key === key ? { ...row, ...patch } : row));
}

// 끌어 오는 것이 파일일 때만 받는다. 줄 순서 바꾸기도 같은 drag 이벤트를 쓴다.
function hasFiles(e: DragEvent): boolean {
  return Array.from(e.dataTransfer.types).includes("Files");
}

// 사진 목록: 첫 장이 대표 사진. 파일은 고르면 바로 올라가고, 설명·출처와 등록은 저장 때 보낸다.
export function PhotosEditor({
  rows,
  savedKeys,
  onChange,
  onFiles,
  onRetry,
  onBlur,
  errors,
}: {
  rows: PhotoRow[];
  savedKeys: Set<string>;
  onChange: (rows: PhotoRow[]) => void;
  onFiles: (files: File[]) => void;
  onRetry: (key: string) => void;
  onBlur: (key: string) => void;
  errors: Record<string, string>;
}) {
  const input = useRef<HTMLInputElement>(null);
  const dialog = useRef<HTMLDialogElement>(null);
  const [rejected, setRejected] = useState<{ name: string; reason: string }[]>([]);
  const [dropping, setDropping] = useState(false);
  const [zoom, setZoom] = useState<string | null>(null);

  useEffect(() => {
    if (zoom) dialog.current?.showModal();
  }, [zoom]);

  // 변환 전에 거른다. 거른 파일은 줄을 만들지 않고 이유만 보인다.
  function pick(files: File[]) {
    const reasons = files.map((file) => ({ file, reason: checkPhotoFile(file) }));
    setRejected(reasons.filter((r) => r.reason).map((r) => ({ name: r.file.name, reason: r.reason as string })));
    const accepted = reasons.filter((r) => !r.reason).map((r) => r.file);
    if (accepted.length > 0) onFiles(accepted);
  }

  function applySource(from: PhotoRow) {
    onChange(rows.map((row) => ({ ...row, credit: from.credit, license: from.license, sourceUrl: from.sourceUrl })));
  }

  return (
    <div
      className={`${styles.photoDrop} ${dropping ? styles.dropping : ""}`}
      onDragOver={(e) => {
        if (!hasFiles(e)) return;
        e.preventDefault();
        setDropping(true);
      }}
      onDragLeave={(e) => {
        if (!e.currentTarget.contains(e.relatedTarget as Node | null)) setDropping(false);
      }}
      onDrop={(e) => {
        if (!hasFiles(e)) return;
        e.preventDefault();
        setDropping(false);
        pick(Array.from(e.dataTransfer.files));
      }}
    >
      <p className={`label-sm ${styles.sub}`}>
        {dropping ? "여기에 놓으면 올라가요" : "JPEG·PNG·WebP · 20MB까지 · 가로 1600px 이상 권장 · 끌어다 놓아도 돼요"}
      </p>
      {rejected.length > 0 && (
        <ul className={styles.rejected} role="alert">
          {rejected.map((r, i) => (
            <li key={i} className={`body-sm ${styles.error}`}>
              {r.name}: {r.reason}
            </li>
          ))}
        </ul>
      )}
      <input
        ref={input}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        multiple
        hidden
        onChange={(e) => {
          pick(Array.from(e.target.files ?? []));
          // 같은 파일을 다시 골라도 change가 오게 비운다.
          e.target.value = "";
        }}
      />
      {rows.length === 0 ? (
        // 사진이 없을 때는 고르기·끌어다 놓기 자리를 크게 둔다.
        <button type="button" className={styles.dropBox} onClick={() => input.current?.click()}>
          <span className="label-md">사진을 끌어다 놓거나 눌러서 고르기</span>
          <span className={`label-sm ${styles.sub}`}>여러 장을 한 번에 골라도 돼요 · 첫 장이 대표 사진</span>
        </button>
      ) : (
        <ReorderList
          label="사진"
          rows={rows}
          onChange={onChange}
          addLabel="사진 더하기 +"
          onAdd={() => input.current?.click()}
          render={(row, i) => {
            const at = (field: string) => `photos[${i}].${field}`;
            const fresh = row.upload === "done" && row.objectKey !== null && !savedKeys.has(row.objectKey);
            const failed = row.upload === "failed" || Boolean(errors[at("objectKey")]);
            const state =
              row.upload === "failed"
                ? (row.uploadError ?? "올리지 못했어요")
                : (errors[at("objectKey")] ?? (fresh ? "올렸어요 · 저장하면 등록돼요" : UPLOAD_LABEL[row.upload]));
            const warning = photoWarning(row.width);
            const required = photoPublishFields(row);
            return (
              <div className={styles.rowFields}>
                <div className={styles.photoHead}>
                  <button
                    type="button"
                    className={styles.thumb}
                    disabled={!row.preview}
                    onClick={() => setZoom(row.full || row.preview)}
                    aria-label={`사진 ${i + 1} 크게 보기`}
                  >
                    {i === 0 && <span className={`label-sm ${styles.photoTag}`}>대표 사진</span>}
                    {row.preview && (
                      // eslint-disable-next-line @next/next/no-img-element -- 저장소의 WebP나 방금 바꾼 파일을 그대로 보인다
                      <img
                        src={row.preview}
                        srcSet={row.full ? `${row.preview} 800w, ${row.full} 1600w` : undefined}
                        sizes="(max-width: 560px) 100vw, 560px"
                        alt=""
                        className={styles.thumbImage}
                      />
                    )}
                  </button>
                  {(state || warning || (row.upload === "failed" && row.file)) && (
                    <div className={styles.photoState}>
                      {state && <span className={`body-sm ${failed ? styles.error : styles.sub}`}>{state}</span>}
                      {warning && <span className={`body-sm ${styles.sub}`}>{warning}</span>}
                      {row.upload === "failed" && row.file && (
                        <button type="button" className={`label-sm ${styles.textButton}`} onClick={() => onRetry(row.key)}>
                          다시 올리기
                        </button>
                      )}
                    </div>
                  )}
                </div>
                <PlaceFormField
                  field={{
                    key: at("alt"),
                    label: "사진 설명",
                    help: "200자까지 · 화면을 읽어 주는 기능이 읽어요",
                    required: required.has("alt"),
                  }}
                  value={row.alt}
                  error={errors[at("alt")]}
                  onChange={(alt) => onChange(update(rows, row.key, { alt }))}
                  onBlur={() => onBlur(at("alt"))}
                />
                <div className={styles.pair}>
                  <PlaceFormField
                    field={{
                      key: at("credit"),
                      label: "사진 출처(이름)",
                      help: "“사진: 이름”으로 보여요",
                      required: required.has("credit"),
                    }}
                    value={row.credit}
                    error={errors[at("credit")]}
                    onChange={(credit) => onChange(update(rows, row.key, { credit }))}
                    onBlur={() => onBlur(at("credit"))}
                  />
                  <PlaceFormField
                    field={{ key: at("license"), label: "사진 종류", type: "select", options: PHOTO_LICENSE_LABEL }}
                    value={row.license}
                    error={errors[at("license")]}
                    onChange={(license) => onChange(update(rows, row.key, { license: license as PhotoRow["license"] }))}
                    onBlur={() => onBlur(at("license"))}
                  />
                </div>
                <PlaceFormField
                  field={{
                    key: at("sourceUrl"),
                    label: "출처 링크",
                    placeholder: "https://",
                    help: "직접 찍은 사진이 아니면 필요해요 · “사진: 이름”에 걸리는 링크",
                    required: required.has("sourceUrl"),
                  }}
                  value={row.sourceUrl}
                  error={errors[at("sourceUrl")]}
                  onChange={(sourceUrl) => onChange(update(rows, row.key, { sourceUrl }))}
                  onBlur={() => onBlur(at("sourceUrl"))}
                />
                {rows.length > 1 && (
                  <div>
                    <button type="button" className={`label-sm ${styles.textButton}`} onClick={() => applySource(row)}>
                      이 출처를 모든 사진에 적용
                    </button>
                  </div>
                )}
              </div>
            );
          }}
        />
      )}
      {/* 확대: 원본 비율 그대로. 누르거나 Esc로 닫는다. */}
      <dialog
        ref={dialog}
        className={styles.zoom}
        aria-label="사진 크게 보기"
        onClose={() => setZoom(null)}
        onClick={() => dialog.current?.close()}
      >
        {zoom && (
          // eslint-disable-next-line @next/next/no-img-element -- 확대는 1600px 파일을 그대로 보인다
          <img src={zoom} alt="" className={styles.zoomImage} />
        )}
      </dialog>
    </div>
  );
}
