import form from "@/shared/ui/Form.module.css";

export type FieldSpec = {
  key: string;
  label: string;
  help?: string;
  placeholder?: string;
  type?: "text" | "decimal" | "month" | "textarea" | "select";
  rows?: number;
  options?: Record<string, string>;
  readOnly?: boolean;
};

// 칸 하나: 라벨 → 입력 → 도움말 → 메시지. 길이는 막지 않고 넘으면 메시지로 알린다.
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
  const id = `place-${field.key.replace(".", "-")}`;
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
      <label htmlFor={id} className={`label-sm ${form.label}`}>
        {field.label}
      </label>
      {field.type === "textarea" ? (
        <textarea
          {...common}
          rows={field.rows}
          className={`body-md ${form.textarea}`}
          onChange={(e) => onChange(e.target.value)}
        />
      ) : field.type === "select" ? (
        <select {...common} className={`body-md ${form.select}`} onChange={(e) => onChange(e.target.value)}>
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
          inputMode={field.type === "decimal" ? "decimal" : undefined}
          className={`body-md ${form.input}`}
          onChange={(e) => onChange(e.target.value)}
        />
      )}
      {field.help && (
        <p id={`${id}-help`} className={`label-sm ${form.help}`}>
          {field.help}
        </p>
      )}
      {error && (
        <p id={`${id}-error`} className={`body-sm ${form.error}`}>
          {error}
        </p>
      )}
    </div>
  );
}
