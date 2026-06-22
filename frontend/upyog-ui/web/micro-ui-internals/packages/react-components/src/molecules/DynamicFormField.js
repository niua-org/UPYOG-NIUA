import React from "react";
import { TextInput, Dropdown, LabelFieldPair, CardLabel, CardLabelError } from "@nudmcdgnpm/digit-ui-react-components";

/**
 * dynamicFormField
 * Renders a single form field based on the field config from FormConfig JSON.
 * Supports: text, dropdown, group (renders children recursively)
 *
 * @param {Object}   fieldConfig   - One item from the form[] array in the config
 * @param {Object}   formData      - Current form state { fieldName: value }
 * @param {Function} onChange      - (fieldName, value) => void
 * @param {Object}   errors        - { fieldName: errorMessage | boolean }
 * @param {Object}   dropdownData  - { fieldCode: [{ code, i18nKey }] } for dropdown options
 * @param {Function} t             - i18n translation function
 * @param {Boolean}  isDisabled    - Override all fields as disabled
 */
const DynamicFormField = ({ fieldConfig, formData, onChange, errors, dropdownData = {}, t, isDisabled = false }) => {
  if (!fieldConfig) return null;

  // ── GROUP type: render children side-by-side ─────────────────────────────
  if (fieldConfig.type === "group") {
    return (
      <div className="dynamic-form-group">
        <CardLabel>
          {t(fieldConfig.label?.code || fieldConfig.key)}
          {fieldConfig.label?.unit && <span className="dynamic-form-unit">{fieldConfig.label.unit}</span>}
        </CardLabel>
        <div className="dynamic-form-group-children">
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
        {errors[fieldConfig.key] && (
          <CardLabelError>{t(fieldConfig.messages?.error || fieldConfig.key + "_ERROR")}</CardLabelError>
        )}
      </div>
    );
  }

  const { field, validation = {}, messages = {} } = fieldConfig;
  if (!field) return null;

  const { name, type, placeholder, unit, code } = field;
  const value = formData[name] ?? "";
  const hasError = !!errors[name];
  const disabled = isDisabled || validation.disabled || false;
  const readOnly = validation.readOnly || false;

  // ── Shared label ─────────────────────────────────────────────────────────
  const label = (
    <CardLabel>
      {t(code || fieldConfig.key)}
      {unit && <span className="dynamic-form-unit"> {unit}</span>}
      {validation.required && <span className="dynamic-form-required">*</span>}
    </CardLabel>
  );

  // ── TEXT input ────────────────────────────────────────────────────────────
  if (type === "text") {
    return (
      <LabelFieldPair key={fieldConfig.key}>
        {label}
        <div className="dynamic-form-field-wrap">
          <TextInput
            name={name}
            value={value}
            placeholder={t(placeholder)}
            disabled={disabled}
            readOnly={readOnly}
            maxLength={validation.maxLength}
            onChange={(e) => {
              let val = e.target.value;
              // Apply regex sanitisation from config (e.g. strip non-numeric chars)
              if (validation.regex?.pattern) {
                const rgx = new RegExp(validation.regex.pattern, validation.regex.flags || "g");
                val = val.replace(rgx, "");
              }
              onChange(name, val);
            }}
            className={`${hasError} ? custom-error : ''`}
          />
          {hasError && (
            <CardLabelError>{t(messages.error || code + "_ERROR")}</CardLabelError>
          )}
        </div>
      </LabelFieldPair>
    );
  }

  // ── DROPDOWN ──────────────────────────────────────────────────────────────
  if (type === "dropdown") {
    const options = dropdownData[code] || [];
    const selected = options.find((o) => o.code === value) || null;
    
    return (
      <LabelFieldPair key={fieldConfig.key}>
        {label}
        <div className="dynamic-form-field-wrap">
          <Dropdown
            placeholder={t(placeholder)}
            selected={selected}
            option={fieldConfig.options}
            optionKey="i18nKey"
            select={(val) => onChange(name, val?.code || "")}
            disable={disabled}
            t={t}
          />
          {hasError && (
            <CardLabelError>{t(messages.error || code + "_ERROR")}</CardLabelError>
          )}
        </div>
      </LabelFieldPair>
    );
  }

  // Unknown type – render nothing (extend here for date, file, etc.)
  return null;
};

export default DynamicFormField;
