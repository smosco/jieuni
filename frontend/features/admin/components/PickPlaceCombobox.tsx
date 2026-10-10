"use client";

import { useEffect, useId, useState, type KeyboardEvent } from "react";
import form from "@/shared/ui/Form.module.css";
import { dayLabel } from "../pickSchema";
import { usePublishedPlaceSearch } from "../queries";
import type { AdminPlaceSummary } from "../types";
import styles from "./PlaceForm.module.css";

export type PickedPlace = { id: number | null; name: string };

// 공개된 공간을 이름으로 찾아 고른다. 다른 날에 이미 편성된 공간은 보이지만 고를 수 없다(공간은 한 번만 편성한다).
export function PickPlaceCombobox({
  id,
  date,
  value,
  onChange,
  error,
}: {
  id: string;
  date: string;
  value: PickedPlace;
  onChange: (value: PickedPlace) => void;
  error?: string;
}) {
  const listId = useId();
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(0);
  const [query, setQuery] = useState("");

  const text = value.name;
  // 칠 때마다 부르지 않게 잠깐 기다린다.
  useEffect(() => {
    const timer = setTimeout(() => setQuery(text.trim()), 200);
    return () => clearTimeout(timer);
  }, [text]);

  const { data } = usePublishedPlaceSearch(query, open);
  const found = data?.items ?? [];
  const taken = (place: AdminPlaceSummary) => place.pickedOn !== null && place.pickedOn !== date;

  function pick(index: number) {
    const place = found[index];
    if (!place || taken(place)) return;
    onChange({ id: place.id, name: place.name });
    setOpen(false);
  }

  function key(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "ArrowDown" || e.key === "ArrowUp") {
      e.preventDefault();
      setOpen(true);
      if (found.length > 0) setActive((a) => (a + (e.key === "ArrowDown" ? 1 : found.length - 1)) % found.length);
    } else if (e.key === "Enter" && open && found.length > 0) {
      e.preventDefault();
      pick(active);
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  const describedBy = [error && `${id}-error`, `${id}-help`].filter(Boolean).join(" ");

  return (
    <div className={`${form.field} ${styles.combobox}`}>
      <label htmlFor={id} className={`label-sm ${form.label}`}>
        공간
      </label>
      <input
        id={id}
        type="text"
        role="combobox"
        autoComplete="off"
        aria-expanded={open && found.length > 0}
        aria-controls={listId}
        aria-activedescendant={open && found.length > 0 ? `${listId}-${active}` : undefined}
        aria-invalid={Boolean(error)}
        aria-describedby={describedBy}
        value={text}
        placeholder="공개된 공간 이름으로 찾기"
        className={`body-md ${form.input}`}
        onChange={(e) => {
          onChange({ id: null, name: e.target.value });
          setOpen(true);
          setActive(0);
        }}
        onFocus={() => setOpen(true)}
        onBlur={() => setOpen(false)}
        onKeyDown={key}
      />
      {open && found.length > 0 && (
        <ul id={listId} role="listbox" className={styles.options}>
          {found.map((place, i) => (
            <li
              key={place.id}
              id={`${listId}-${i}`}
              role="option"
              aria-selected={i === active}
              aria-disabled={taken(place)}
              className={`${styles.option} ${i === active ? styles.optionActive : ""} ${taken(place) ? styles.optionOff : ""}`}
              onMouseDown={(e) => e.preventDefault()}
              onClick={() => pick(i)}
            >
              <span className="body-md">{place.name}</span>
              <span className={`label-sm ${styles.sub}`}>
                {[place.region, place.pickedOn && taken(place) && `${dayLabel(place.pickedOn)} 편성됨`]
                  .filter(Boolean)
                  .join(" · ")}
              </span>
            </li>
          ))}
        </ul>
      )}
      <p id={`${id}-help`} className={`label-sm ${form.help}`}>
        {value.id ? "골랐어요" : "목록에서 골라요"}
      </p>
      {error && (
        <p id={`${id}-error`} className={`body-sm ${form.error}`}>
          {error}
        </p>
      )}
    </div>
  );
}
