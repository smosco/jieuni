"use client";

import { useState, type FormEvent } from "react";
import { ApiError } from "@/shared/api/client";
import form from "@/shared/ui/Form.module.css";
import { SectionLabel } from "@/shared/ui/SectionLabel";
import { LINE_MAX, WEEKDAY_LABEL, dayLabel, pickErrors } from "../pickSchema";
import { useAdminDashboard, useDeletePick, usePutPick } from "../queries";
import type { AdminPick } from "../types";
import { AdminHead } from "./AdminHead";
import { PickPlaceCombobox, type PickedPlace } from "./PickPlaceCombobox";
import { PlaceFormField } from "./PlaceFormField";
import admin from "./Admin.module.css";
import styles from "./PlaceForm.module.css";

// 이번 주·다음 주 월~금 칸. 어느 날이 이번 주인지, 칸이 열렸는지(locked)는 서버가 정한다.
export function AdminPicks() {
  const { data, isError } = useAdminDashboard();
  // 한 번에 한 칸만 고친다.
  const [editing, setEditing] = useState<string | null>(null);

  return (
    <>
      <AdminHead title="편성" />
      <p className={`body-sm ${admin.lead}`}>
        칸마다 공개된 공간 하나와 한 줄({LINE_MAX}자)을 넣어요. 그 날 08:00에 피드에 열리고, 열린 칸은 고칠 수 없어요.
      </p>
      {isError && <p className={`body-md ${admin.empty}`}>불러오지 못했어요. 다시 시도해 주세요</p>}
      {data && (
        <div className={admin.pickWeeks}>
          {(
            [
              ["이번 주", data.picks.thisWeek],
              ["다음 주", data.picks.nextWeek],
            ] as const
          ).map(([title, picks]) => (
            <section key={title} className={admin.week}>
              <SectionLabel>{title}</SectionLabel>
              <ul className={admin.slots}>
                {picks.map((pick) => (
                  <li key={pick.date}>
                    <PickSlot
                      pick={pick}
                      editing={editing === pick.date}
                      onEdit={() => setEditing(pick.date)}
                      onClose={() => setEditing(null)}
                    />
                  </li>
                ))}
              </ul>
            </section>
          ))}
        </div>
      )}
    </>
  );
}

function PickSlot({
  pick,
  editing,
  onEdit,
  onClose,
}: {
  pick: AdminPick;
  editing: boolean;
  onEdit: () => void;
  onClose: () => void;
}) {
  const remove = useDeletePick();
  const [notice, setNotice] = useState<string | null>(null);

  function clear() {
    setNotice(null);
    remove.mutate(pick.date, { onError: (err) => setNotice(failure(err)) });
  }

  return (
    <div className={admin.slot}>
      <div className={admin.slotRow}>
        <p className={`label-md ${admin.slotDay}`}>
          {WEEKDAY_LABEL[pick.weekday]} {dayLabel(pick.date)}
        </p>
        <div className={admin.slotMain}>
          {pick.place ? (
            <>
              <p className={`body-md ${admin.rowName}`}>{pick.place.name}</p>
              <p className={`body-sm ${admin.slotLine}`}>{pick.line}</p>
            </>
          ) : (
            // 아직 채울 수 있는 빈 칸만 강조한다.
            <p className={`body-md ${pick.locked ? admin.slotPast : admin.slotEmpty}`}>
              {pick.locked ? "편성 없이 지났어요" : "비어 있어요"}
            </p>
          )}
        </div>
        <div className={admin.slotActions}>
          {pick.locked ? (
            <span className={`label-sm ${admin.meta}`}>열림</span>
          ) : (
            !editing && (
              <>
                <button type="button" className={`label-sm ${styles.textButton}`} onClick={onEdit}>
                  {pick.place ? "바꾸기" : "넣기"}
                </button>
                {pick.place && (
                  <button type="button" className={`label-sm ${styles.textButton}`} disabled={remove.isPending} onClick={clear}>
                    비우기
                  </button>
                )}
              </>
            )
          )}
        </div>
      </div>
      {notice && <p className={`body-sm ${styles.error}`}>{notice}</p>}
      {editing && <PickEditor pick={pick} onClose={onClose} />}
    </div>
  );
}

function PickEditor({ pick, onClose }: { pick: AdminPick; onClose: () => void }) {
  const put = usePutPick();
  const [place, setPlace] = useState<PickedPlace>(
    pick.place ? { id: pick.place.id, name: pick.place.name } : { id: null, name: "" },
  );
  const [line, setLine] = useState(pick.line ?? "");
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [notice, setNotice] = useState<string | null>(null);

  function save(e: FormEvent) {
    e.preventDefault();
    const found = pickErrors(place.id, line);
    setErrors(found);
    setNotice(null);
    if (place.id === null || Object.keys(found).length > 0) return;
    put.mutate(
      { date: pick.date, placeId: place.id, line: line.trim() },
      {
        onSuccess: onClose,
        onError: (err) => {
          if (err instanceof ApiError && err.fieldErrors) setErrors(err.fieldErrors);
          setNotice(failure(err));
        },
      },
    );
  }

  return (
    <form className={admin.slotEditor} onSubmit={save} noValidate>
      <PickPlaceCombobox id={`pick-${pick.date}-place`} date={pick.date} value={place} onChange={setPlace} error={errors.placeId} />
      <PlaceFormField
        field={{ key: `pick-${pick.date}-line`, label: "한 줄", help: `${LINE_MAX}자까지 · 피드 카드에 보여요` }}
        value={line}
        error={errors.line}
        onChange={setLine}
      />
      <div className={admin.slotButtons}>
        <button type="submit" className={`label-md ${form.primary}`} disabled={put.isPending}>
          {put.isPending ? "저장 중…" : "저장"}
        </button>
        <button type="button" className={`label-sm ${styles.textButton}`} onClick={onClose}>
          취소
        </button>
        {notice && <p className={`body-sm ${styles.error}`}>{notice}</p>}
      </div>
    </form>
  );
}

// 서버 문구(409 "이미 열린 날은 바꿀 수 없어요" 등)를 그대로 보인다.
function failure(err: unknown): string {
  return err instanceof ApiError && err.status < 500 ? err.message : "저장하지 못했어요. 다시 시도해 주세요";
}
