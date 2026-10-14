import form from "@/shared/ui/Form.module.css";

// 서버 fieldErrors 키("photos[0].alt", "senses.light")로 칸 id를 만든다. 첫 오류 칸으로 옮겨 갈 때 같은 id를 찾는다.
export function fieldId(key: string): string {
  return `place-${key.replace(/[.[\]]+/g, "-").replace(/-$/, "")}`;
}

// required는 공개에 필요한 칸이다. 표시는 라벨 뒤 * 하나뿐이고, 저장은 막지 않는다.
export function FieldLabel({ htmlFor, label, required }: { htmlFor: string; label: string; required?: boolean }) {
  return (
    <label htmlFor={htmlFor} className={`label-md ${form.strongLabel}`}>
      {label}
      {required && <span aria-hidden="true"> *</span>}
    </label>
  );
}

export type FieldSpec = {
  key: string;
  label: string;
  help?: string;
  placeholder?: string;
  type?: "text" | "month" | "textarea" | "select";
  rows?: number;
  options?: Record<string, string>;
  readOnly?: boolean;
  required?: boolean;
};

// 칸 하나: 라벨 → 입력 → 메시지 → 도움말. 메시지는 칸 바로 아래에 붙인다. 길이는 막지 않고 넘으면 메시지로 알린다.
export function PlaceFormField({
  field,
  value,
  error,
  onChange,
  onBlur,
}: {
  field: FieldSpec;
  value: string;
  error?: string;
  onChange: (value: string) => void;
  onBlur?: () => void;
}) {
  const id = fieldId(field.key);
  const describedBy = [error && `${id}-error`, field.help && `${id}-help`].filter(Boolean).join(" ") || undefined;
  const common = {
    id,
    name: field.key,
    value,
    placeholder: field.placeholder,
    readOnly: field.readOnly,
    "aria-invalid": Boolean(error),
    "aria-describedby": describedBy,
    onBlur,
  };

  return (
    <div className={form.field}>
      <FieldLabel htmlFor={id} label={field.label} required={field.required} />
      {field.type === "textarea" ? (
        <textarea
          {...common}
          rows={field.rows}
          className={`body-md ${form.textarea} ${form.raised}`}
          onChange={(e) => onChange(e.target.value)}
        />
      ) : field.type === "select" ? (
        <select
          {...common}
          className={`body-md ${form.select} ${form.raised}`}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">골라 주세요</option>
          {Object.entries(field.options ?? {}).map(([code, label]) => (
            <option key={code} value={code}>
              {label}
            </option>
          ))}
        </select>
      ) : (
        <input
          {...common}
          type={field.type === "month" ? "month" : "text"}
          className={`body-md ${form.input} ${form.raised}`}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {error && (
        <p id={`${id}-error`} className={`body-sm ${form.error}`}>
          {error}
        </p>
      )}
      {field.help && (
        <p id={`${id}-help`} className={`label-sm ${form.help}`}>
          {field.help}
        </p>
      )}
    </div>
  );
}
