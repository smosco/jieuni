"use client";

import { useEffect, useRef, useState, type DragEvent, type ReactNode } from "react";
import { move } from "../listSchema";
import styles from "./PlaceForm.module.css";

// 순서가 의미 있는 목록: 손잡이를 끌어 놓거나 ↑↓로 옮긴다. 끌기는 브라우저 기본 drag라 손잡이에서 시작할 때만 켠다
// (줄 전체를 draggable로 두면 입력칸 안의 글자를 고를 수 없다).
export function ReorderList<T extends { key: string }>({
  label,
  rows,
  onChange,
  render,
  onAdd,
  addLabel,
}: {
  label: string;
  rows: T[];
  onChange: (rows: T[]) => void;
  render: (row: T, index: number) => ReactNode;
  onAdd: () => void;
  addLabel: string;
}) {
  const [armed, setArmed] = useState<string | null>(null);
  // 끄는 줄은 렌더와 상관없이 바로 읽어야 해서 ref에 둔다(dragstart 직후의 dragover가 옛 state를 보지 않게).
  const from = useRef<number | null>(null);
  const [over, setOver] = useState<number | null>(null);

  // 손잡이를 누른 채 밖으로 나가 떼도 풀리게 window에서 받는다. 끌기가 시작될 때 오는 pointercancel은 끄는 중이라 무시한다.
  useEffect(() => {
    if (armed === null) return;
    const release = () => {
      if (from.current === null) setArmed(null);
    };
    window.addEventListener("pointerup", release);
    window.addEventListener("pointercancel", release);
    return () => {
      window.removeEventListener("pointerup", release);
      window.removeEventListener("pointercancel", release);
    };
  }, [armed]);

  function start(e: DragEvent, index: number) {
    from.current = index;
    e.dataTransfer.effectAllowed = "move";
    // 파이어폭스는 데이터가 없으면 끌기를 시작하지 않는다.
    e.dataTransfer.setData("text/plain", rows[index].key);
  }

  function drop(e: DragEvent, index: number) {
    e.preventDefault();
    if (from.current !== null) onChange(move(rows, from.current, index));
    end();
  }

  function end() {
    setArmed(null);
    from.current = null;
    setOver(null);
  }

  return (
    <div className={styles.listBlock}>
      {rows.length === 0 ? (
        <p className={`body-sm ${styles.none}`}>아직 없어요</p>
      ) : (
        <ol className={styles.list} aria-label={label}>
          {rows.map((row, i) => (
            <li
              key={row.key}
              className={`${styles.item} ${over === i && armed !== row.key ? styles.dropTarget : ""}`}
              draggable={armed === row.key}
              onDragStart={(e) => start(e, i)}
              onDragOver={(e) => {
                if (from.current === null) return;
                e.preventDefault();
                setOver(i);
              }}
              onDrop={(e) => drop(e, i)}
              onDragEnd={end}
            >
              <div className={styles.itemHead}>
                <span
                  className={`label-sm ${styles.handle}`}
                  onPointerDown={() => setArmed(row.key)}
                  title="끌어서 순서 바꾸기"
                  aria-hidden="true"
                >
                  ⋮⋮ {i + 1}
                </span>
                <div className={styles.itemActions}>
                  <button
                    type="button"
                    className={`label-sm ${styles.textButton}`}
                    disabled={i === 0}
                    onClick={() => onChange(move(rows, i, i - 1))}
                    aria-label={`${i + 1}번째를 위로`}
                  >
                    위로
                  </button>
                  <button
                    type="button"
                    className={`label-sm ${styles.textButton}`}
                    disabled={i === rows.length - 1}
                    onClick={() => onChange(move(rows, i, i + 1))}
                    aria-label={`${i + 1}번째를 아래로`}
                  >
                    아래로
                  </button>
                  <button
                    type="button"
                    className={`label-sm ${styles.textButton}`}
                    onClick={() => onChange(rows.filter((r) => r.key !== row.key))}
                    aria-label={`${i + 1}번째 빼기`}
                  >
                    빼기
                  </button>
                </div>
              </div>
              {render(row, i)}
            </li>
          ))}
        </ol>
      )}
      <div>
        <button type="button" className={`label-md ${styles.textButton}`} onClick={onAdd}>
          {addLabel}
        </button>
      </div>
    </div>
  );
}
