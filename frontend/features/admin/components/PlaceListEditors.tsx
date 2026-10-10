"use client";

import { embedTypeOf } from "@/shared/lib/embed";
import { ROLE_LABEL } from "@/shared/lib/labels";
import form from "@/shared/ui/Form.module.css";
import { SOURCE_TYPE_LABEL } from "../labels";
import { rowKey, type CreditRow, type EmbedRow, type SourceRow } from "../listSchema";
import { MakerCombobox } from "./MakerCombobox";
import { PlaceFormField } from "./PlaceFormField";
import { ReorderList } from "./ReorderList";
import styles from "./PlaceForm.module.css";

type Editor<T> = {
  rows: T[];
  onChange: (rows: T[]) => void;
  errors: Record<string, string>;
};

function update<T extends { key: string }>(rows: T[], key: string, patch: Partial<T>): T[] {
  return rows.map((row) => (row.key === key ? { ...row, ...patch } : row));
}

export function CreditsEditor({ rows, onChange, errors }: Editor<CreditRow>) {
  return (
    <ReorderList
      label="크레딧"
      rows={rows}
      onChange={onChange}
      addLabel="크레딧 더하기 +"
      onAdd={() =>
        onChange([...rows, { key: rowKey(), makerId: null, makerName: "", role: "", verified: false, sourceUrl: "" }])
      }
      render={(row, i) => {
        const at = (field: string) => `credits[${i}].${field}`;
        return (
          <div className={styles.rowFields}>
            <div className={styles.pair}>
              <MakerCombobox
                id={`credit-${row.key}-maker`}
                name={at("makerId")}
                label="만든 사람"
                value={{ id: row.makerId, name: row.makerName }}
                onChange={(maker) => onChange(update(rows, row.key, { makerId: maker.id, makerName: maker.name }))}
                error={errors[at("makerId")]}
                allowCreate
              />
              <PlaceFormField
                field={{ key: at("role"), label: "역할", type: "select", options: ROLE_LABEL }}
                value={row.role}
                error={errors[at("role")]}
                onChange={(role) => onChange(update(rows, row.key, { role: role as CreditRow["role"] }))}
              />
            </div>
            <PlaceFormField
              field={{ key: at("sourceUrl"), label: "출처 링크", placeholder: "https://", help: "공식 소개·기사 등 만든 사람을 확인한 곳" }}
              value={row.sourceUrl}
              error={errors[at("sourceUrl")]}
              onChange={(sourceUrl) => onChange(update(rows, row.key, { sourceUrl }))}
            />
            <label className={`body-md ${form.check}`}>
              <input
                type="checkbox"
                name={at("verified")}
                checked={row.verified}
                onChange={(e) => onChange(update(rows, row.key, { verified: e.target.checked }))}
              />
              출처로 확인했어요
            </label>
          </div>
        );
      }}
    />
  );
}

export function SourcesEditor({ rows, onChange, errors }: Editor<SourceRow>) {
  return (
    <ReorderList
      label="출처"
      rows={rows}
      onChange={onChange}
      addLabel="출처 더하기 +"
      onAdd={() => onChange([...rows, { key: rowKey(), title: "", url: "", type: "" }])}
      render={(row, i) => {
        const at = (field: string) => `sources[${i}].${field}`;
        return (
          <div className={styles.rowFields}>
            <PlaceFormField
              field={{ key: at("title"), label: "제목", help: "200자까지" }}
              value={row.title}
              error={errors[at("title")]}
              onChange={(title) => onChange(update(rows, row.key, { title }))}
            />
            <div className={styles.pair}>
              <PlaceFormField
                field={{ key: at("url"), label: "링크", placeholder: "https://" }}
                value={row.url}
                error={errors[at("url")]}
                onChange={(url) => onChange(update(rows, row.key, { url }))}
              />
              <PlaceFormField
                field={{ key: at("type"), label: "종류", type: "select", options: SOURCE_TYPE_LABEL }}
                value={row.type}
                error={errors[at("type")]}
                onChange={(type) => onChange(update(rows, row.key, { type: type as SourceRow["type"] }))}
              />
            </div>
          </div>
        );
      }}
    />
  );
}

const EMBED_LABEL = { youtube: "유튜브 영상", instagram: "인스타그램 게시물" } as const;

export function EmbedsEditor({ rows, onChange, errors }: Editor<EmbedRow>) {
  return (
    <ReorderList
      label="임베드"
      rows={rows}
      onChange={onChange}
      addLabel="임베드 더하기 +"
      onAdd={() => onChange([...rows, { key: rowKey(), url: "" }])}
      render={(row, i) => {
        const type = embedTypeOf(row.url.trim());
        return (
          <PlaceFormField
            field={{
              key: `embeds[${i}].url`,
              label: "공개 게시물·영상 주소",
              placeholder: "https://www.instagram.com/p/…",
              help: type ? EMBED_LABEL[type] : "인스타그램 게시물이나 유튜브 영상 주소",
            }}
            value={row.url}
            error={errors[`embeds[${i}].url`]}
            onChange={(url) => onChange(update(rows, row.key, { url }))}
          />
        );
      }}
    />
  );
}
