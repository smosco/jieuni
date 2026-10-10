"use client";

import { useRef } from "react";
import { PHOTO_LICENSE_LABEL } from "../labels";
import type { PhotoRow } from "../photoSchema";
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

// 사진 목록: 첫 장이 대표 사진. 파일은 고르면 바로 올라가고, 설명·출처와 등록은 임시저장 때 보낸다.
export function PhotosEditor({
  rows,
  savedKeys,
  onChange,
  onFiles,
  onRetry,
  errors,
}: {
  rows: PhotoRow[];
  savedKeys: Set<string>;
  onChange: (rows: PhotoRow[]) => void;
  onFiles: (files: File[]) => void;
  onRetry: (key: string) => void;
  errors: Record<string, string>;
}) {
  const input = useRef<HTMLInputElement>(null);

  return (
    <>
      <input
        ref={input}
        type="file"
        accept="image/*"
        multiple
        hidden
        onChange={(e) => {
          onFiles(Array.from(e.target.files ?? []));
          // 같은 파일을 다시 골라도 change가 오게 비운다.
          e.target.value = "";
        }}
      />
      <ReorderList
        label="사진"
        rows={rows}
        onChange={onChange}
        addLabel="사진 더하기 +"
        onAdd={() => input.current?.click()}
        render={(row, i) => {
          const at = (field: string) => `photos[${i}].${field}`;
          const fresh = row.upload === "done" && row.objectKey !== null && !savedKeys.has(row.objectKey);
          const state =
            row.upload === "failed"
              ? (row.uploadError ?? "올리지 못했어요")
              : (errors[at("objectKey")] ?? (fresh ? "올렸어요 · 임시저장하면 등록돼요" : UPLOAD_LABEL[row.upload]));
          return (
            <div className={styles.rowFields}>
              <div className={styles.photoHead}>
                <div className={styles.thumb}>
                  {row.preview && (
                    // eslint-disable-next-line @next/next/no-img-element -- 저장소의 800px WebP나 방금 바꾼 파일을 그대로 보인다
                    <img src={row.preview} alt="" className={styles.thumbImage} />
                  )}
                </div>
                <div className={styles.photoState}>
                  {i === 0 && <span className="label-sm">대표 사진</span>}
                  {state && <span className={`body-sm ${styles.error}`}>{state}</span>}
                  {row.upload === "failed" && row.file && (
                    <button type="button" className={`label-sm ${styles.textButton}`} onClick={() => onRetry(row.key)}>
                      다시 올리기
                    </button>
                  )}
                </div>
              </div>
              <PlaceFormField
                field={{ key: at("alt"), label: "사진 설명", help: "200자까지 · 화면을 읽어 주는 기능이 읽어요" }}
                value={row.alt}
                error={errors[at("alt")]}
                onChange={(alt) => onChange(update(rows, row.key, { alt }))}
              />
              <div className={styles.pair}>
                <PlaceFormField
                  field={{ key: at("credit"), label: "사진 출처(이름)", help: "“사진: 이름”으로 보여요" }}
                  value={row.credit}
                  error={errors[at("credit")]}
                  onChange={(credit) => onChange(update(rows, row.key, { credit }))}
                />
                <PlaceFormField
                  field={{ key: at("license"), label: "사진 종류", type: "select", options: PHOTO_LICENSE_LABEL }}
                  value={row.license}
                  error={errors[at("license")]}
                  onChange={(license) => onChange(update(rows, row.key, { license: license as PhotoRow["license"] }))}
                />
              </div>
              <PlaceFormField
                field={{
                  key: at("sourceUrl"),
                  label: row.license === "own" ? "출처 링크 (선택)" : "출처 링크",
                  placeholder: "https://",
                  help: "“사진: 이름”에 걸리는 링크",
                }}
                value={row.sourceUrl}
                error={errors[at("sourceUrl")]}
                onChange={(sourceUrl) => onChange(update(rows, row.key, { sourceUrl }))}
              />
            </div>
          );
        }}
      />
    </>
  );
}
