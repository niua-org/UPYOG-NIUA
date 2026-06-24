import React from "react";
import {
  CardLabel,
  Dropdown,
  TextInput,
  LabelFieldPair,
} from "@nudmcdgnpm/digit-ui-react-components";

const DynamicFormField = ({
  fieldConfig,
  formData,
  onChange,
  errors,
  dropdownData = {},
  t,
  isDisabled = false,
}) => {

  // ── GROUP: render children side by side ──────────────────────────────
  if (fieldConfig.type === "group") {
    return (
      <div className="dynamic-form-group">
        <CardLabel>
          {t(fieldConfig.label?.code || fieldConfig.key)}
          {fieldConfig.label?.unit && (
            <span className="field-unit"> {fieldConfig.label.unit}</span>
          )}
        </CardLabel>
        <div style={{ display: "flex", gap: "16px", flexWrap: "wrap" }}>
          {(fieldConfig.children || []).map((child) => (
            <DynamicFormField
              key={child.key}
              fieldConfig={child}
              formData={formData}
              onChange={onChange}
              errors={errors}
              dropdownData={dropdownData}
              t={t}
              isDisabled={isDisabled}
            />
          ))}
        </div>
      </div>
    );
  }

  const { field, validation = {}, messages = {} } = fieldConfig;
  if (!field) return null;

  const { name, type, placeholder, unit } = field;
  const value = formData[name];
  const hasError = errors[name];

  // ── DROPDOWN ─────────────────────────────────────────────────────────

  if (type === "dropdown") {

    const options =
      dropdownData[fieldConfig.key] ||
      (fieldConfig.options || []).map((o) => ({
        code: o.code || o.value,
        name: o.value || o.code,
        value: o.i18nKey || o.value || o.code,
        i18nKey: o.i18nKey || o.code,  // ← i18nKey = name so t() returns name
      }));

    const isFieldDisabled = isDisabled || fieldConfig.key === "EST_CITY";

    // Custom t that returns the value as-is if no translation exists
    const tSafe = (key) => {
      if (!key) return "";
      const translated = t(key);
      // Digit's t() returns the key if no translation found
      // We want to show the actual name, not a code like "JLC476"
      return translated || key;
    };

    return (
      <LabelFieldPair>
        <CardLabel style={{ color: hasError ? "red" : undefined }}>
          {t(fieldConfig.key)}
          {validation.required && <span style={{ color: "red" }}> *</span>}
        </CardLabel>
        <div className="field">
          <Dropdown
            placeholder={tSafe(placeholder || "")}
            selected={value || null}
            option={options}
            optionKey="value"
            select={(val) => onChange(name, val)}
            t={tSafe}
            disable={isFieldDisabled}
          />
          {hasError && (
            <p
              className="field-error"
              style={{ color: "red", fontSize: "12px", marginTop: "4px" }}
            >
              {t(messages.error || "FIELD_REQUIRED")}
            </p>
          )}
        </div>
      </LabelFieldPair>
    );
  }

  // ── TEXT INPUT ────────────────────────────────────────────────────────
  return (
    <LabelFieldPair>
      <CardLabel style={{ color: hasError ? "red" : undefined }}>
        {t(fieldConfig.key)}
        {unit && <span className="field-unit"> {unit}</span>}
        {validation.required && <span style={{ color: "red" }}> *</span>}
      </CardLabel>
      <div className="field">
        <TextInput
          placeholder={t(placeholder || "")}
          value={value || ""}
          onChange={(e) => {
            let val = e.target.value;
            if (validation.regex) {
              val = val.replace(
                new RegExp(validation.regex.pattern, validation.regex.flags || ""),
                ""
              );
            }
            if (validation.maxLength) {
              val = val.slice(0, validation.maxLength);
            }
            onChange(name, val);
          }}
          disabled={isDisabled || validation.disabled}
          readOnly={validation.readOnly}
          style={{ borderColor: hasError ? "red" : undefined }}
        />
        {hasError && (
          <p
            className="field-error"
            style={{ color: "red", fontSize: "12px", marginTop: "4px" }}
          >
            {t(messages.error || "FIELD_REQUIRED")}
          </p>
        )}
      </div>
    </LabelFieldPair>
  );
};

export default DynamicFormField;