/**
 * DynamicFormField.js
 *
 * Single-field (or group / section) renderer used by DynamicForm.
 * Takes one MDMS / routeConfig field definition and renders the matching
 * UI control — text, date, dropdown, radio, file upload, lookup/search card,
 * section header, or nested group — with visibility, labels, and options
 * driven by that config.
 *
 * Responsibilities
 * ----------------
 * 1. Honor fieldConfig.type:
 *    - "sectionHeader" → heading only
 *    - "group"         → label + recursive children in a row (if visible)
 *    - otherwise       → leaf control for field.type
 * 2. Hide leaf / group nodes when isFieldVisible(fieldConfig, formData) is false
 *    (visibleWhen / related visibility rules).
 * 3. Resolve display label via resolveFieldLabelKey (supports dynamic labels).
 * 4. Resolve dropdown / radio options from useDynamicMDMS dropdownData (by
 *    field name or fieldConfig.key), falling back to fieldConfig.options.
 * 5. Apply validation.regex sanitization and maxLength on text input change.
 * 6. Special display for computeFn === "calculateDuration" (years vs months
 *    via formatDurationDisplay).
 * 7. Lookup / search UI when field.searchCard or field.searchButton is set:
 *    text + search button, suggestion list, found card, or not-found / create-new.
 * 8. Memoized with a custom areEqual that only re-renders when watched form
 *    values (getFieldWatchNames), errors, or stable props change.
 *
 * Supported leaf field.type values
 * --------------------------------
 * - dropdown  — Digit Dropdown; selection enriched via enrichDropdownSelection
 * - radio     — native radio group from options
 * - date      — DatePicker (value normalized with toInputDate)
 * - file      — UploadFile; upload via onFileUpload, clear via onChange(null)
 * - text (default) — TextInput; optional search-card variant when searchButton/searchCard
 *
 * Typical MDMS field shape (leaf)
 * -------------------------------
 *   {
 *     key: "EST_BUILDING_NAME",
 *     field: { name: "buildingName", type: "text", placeholder: "..." },
 *     validation: { required: true, maxLength: 100, regex: { pattern, flags } },
 *     messages: { error: "FIELD_REQUIRED" },
 *     label: { code: "EST_BUILDING_NAME" },
 *     visibleWhen: { field: "showRegistrationDetails", value: "YES" },
 *   }
 *
 * Props
 * -----
 * @param {object}   fieldConfig              One form entry from routeConfig.form
 *                                            (or a group child / sectionHeader).
 * @param {object}   formData                 Current DynamicForm state (keyed by field.name).
 * @param {Function} onChange                 (fieldName, value, resetFields?) => void.
 * @param {object}   errors                   Map of fieldName → truthy when invalid.
 * @param {object}   [dropdownData]           MDMS / master options keyed by field name or key.
 * @param {Function} t                        i18n translator.
 * @param {boolean}  [isDisabled=false]       Disables the control (and radio / search actions).
 * @param {Function} [onFileUpload]           (fieldName, file) => void for type === "file".
 * @param {Function} [onFieldSearch]          (fieldName) => void; Enter / search-button trigger.
 * @param {boolean}  [isFieldSearching]       Disables search button while lookup is in flight.
 * @param {object}   [searchPanel]            { fieldName, status, estateNo?, matches?, prefill? }
 *                                            status: "matches" | "found" | "notFound".
 * @param {Function} [onSelectSearchResult]   (fieldName, match?) => void when user picks a hit.
 * @param {Function} [onCreateNewFromSearch]  (fieldName) => void from not-found "create new".
 *
 * @see DynamicForm
 * @see isFieldVisible
 * @see resolveFieldLabelKey
 * @see getFieldWatchNames
 */

import React, { useMemo } from "react";
import {
  CardLabel,
  Dropdown,
  TextInput,
  DatePicker,
  UploadFile,
} from "@nudmcdgnpm/digit-ui-react-components";
import LabelFieldPair from "../atoms/LabelFieldPair";
import {
  toInputDate,
  resolveFieldLabelKey,
  getFieldWatchNames,
  optionCode,
  radioOptionMatches,
  enrichDropdownSelection,
  isFieldVisible,
  getFieldArrayName,
  createEmptyFieldArrayItem,
  resolveFieldDateBounds,
  filterDependsOnOptions,
  evaluateFormRule,
} from "../utilities/formUtils";
import { formatDurationDisplay } from "../utilities/validators";

/** Resolve DatePicker `min` / `max`, including the financial-year clamp. */
const resolveFieldBoundDates = (field) => {
  const { min, max } = resolveFieldDateBounds(field);
  return { min: min || undefined, max: max || undefined };
};

/* ── shared sub-renderers ─────────────────────────────────────────────── */

/**
 * Field label row: translated text, optional unit, required asterisk, error styling.
 * Uses card-label-smaller so LabelFieldPair lays label and control out inline.
 *
 * @param {object}  props
 * @param {string}  props.text      Already-translated label text.
 * @param {boolean} [props.required] Show required asterisk when true.
 * @param {boolean} [props.hasError] Applies error label class when true.
 * @param {string}  [props.unit]    Optional unit suffix (e.g. "sq.ft").
 * @returns {JSX.Element}
 */
const FieldLabel = ({ text, required, hasError, unit }) => (
  <CardLabel
    className={`card-label-smaller${
      hasError ? " dynamic-form-field__label--error" : ""
    }`}
  >
    {text}
    {unit && <span className="dynamic-form-field__unit"> {unit}</span>}
    {required && (
      <span className="astericColor dynamic-form-field__required"> *</span>
    )}
  </CardLabel>
);

/**
 * Inline validation message under a control; renders nothing when `show` is falsy.
 *
 * @param {object}  props
 * @param {boolean} props.show    Whether to render the error.
 * @param {string}  props.message Already-translated error text.
 * @returns {JSX.Element|null}
 */
const FieldError = ({ show, message }) =>
  show ? (
    <p className="dynamic-form-field__error">{message}</p>
  ) : null;

/**
 * Decorative magnifying-glass SVG for the lookup / search-card button.
 * @returns {JSX.Element}
 */
// Todo: icon will move in assets folder
const SearchIcon = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" aria-hidden="true">
    <circle cx="11" cy="11" r="7" stroke="currentColor" strokeWidth="2" />
    <path d="M20 20l-3.5-3.5" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
  </svg>
);

/** Stable empty array so dropdown/radio fields without options don't allocate each render. */
const EMPTY_OPTIONS = [];

/**
 * Sanitizes a text input value using optional regex strip + maxLength, then
 * calls onChange(fieldName, value). Shared by plain text and search-card inputs.
 *
 * @param {Event}    e             Input change event.
 * @param {object}   opts
 * @param {string}   opts.name     field.name to update.
 * @param {RegExp|null} opts.sanitizeRegex  Pattern whose matches are removed.
 * @param {object}   opts.validation        Field validation (maxLength, etc.).
 * @param {Function} opts.onChange          DynamicForm handleChange.
 */
const applyTextInputChange = (e, { name, sanitizeRegex, validation, onChange }) => {
  let val = e.target.value;
  if (sanitizeRegex) val = val.replace(sanitizeRegex, "");
  if (validation.maxLength) val = val.slice(0, validation.maxLength);
  if (validation.maxAmount != null && val !== "") {
    const num = Number(String(val).replace(/,/g, "").trim());
    if (Number.isFinite(num) && num > Number(validation.maxAmount)) {
      val = String(validation.maxAmount);
    }
  }
  onChange(name, val);
};

/**
 * Localization with a JSON fallback. A missing key comes back equal to itself,
 * so the stored default is shown instead of the raw key.
 *
 * @param {Function} t
 * @param {string} key
 * @param {string} [fallback]
 * @returns {string}
 */
const translateWithDefault = (t, key, fallback) => {
  if (!fallback) return t(key);
  const translated = t(key, { defaultValue: fallback });
  return !translated || translated === key ? fallback : translated;
};

/**
 * Single-field (or group / sectionHeader) renderer for DynamicForm.
 * Branches on fieldConfig.type / field.type and returns the matching control.
 *
 * @param {object} props — see file-level Props section above.
 * @returns {JSX.Element|null}
 */
const DynamicFormField = ({
  fieldConfig,
  formData,
  onChange,
  onFieldBlur,
  errors,
  dropdownData = {},
  t,
  isDisabled = false,
  onFileUpload,
  onFieldSearch,
  isFieldSearching = false,
  searchPanel = null,
  onSelectSearchResult,
  onCreateNewFromSearch,
  enrichFieldArrayRow = null,
  /** Unique HTML name for radio groups (required when the same field repeats in a fieldArray). */
  inputName = null,
}) => {
  const { field, validation = {}, messages = {} } = fieldConfig;
  const { name, type, placeholder, unit } = field || {};
  const radioInputName = inputName || name;

  /**
   * Dropdown / radio option list.
   * Prefers MDMS dropdownData[name|key]; falls back to static fieldConfig.options.
   * Applies dependsOn / hierarchy cascade (TradeType category → type → subtype).
   *
   * @returns {object[]}
   */
  const options = useMemo(() => {
    if (type !== "dropdown" && type !== "radio") return EMPTY_OPTIONS;

    const fromMdms = dropdownData[name] || dropdownData[fieldConfig.key];
    let base =
      Array.isArray(fromMdms) && fromMdms.length > 0
        ? fromMdms
        : (fieldConfig.options || []).map((o) => ({
            code: o.code || o.value,
            name: o.name || o.value || o.code,
            value: o.i18nKey || o.localname || o.value || o.code,
            i18nKey: o.i18nKey || o.localname || o.value || o.code,
          }));

    const ds = field?.dataSource;
    if (
      ds &&
      (ds.dependsOn ||
        ds.hierarchy ||
        ds.optionLevel ||
        ds.customiztionRequired ||
        ds.dependsOnSegment != null)
    ) {
      base = filterDependsOnOptions(base, ds, formData);
    }

    const labeled = base.map((o) => {
      const key = o.i18nKey || "";
      const translated = key ? t(key) : "";
      const untranslated = !translated || translated === key;
      // An i18nKey that is not the code or the display name is a locale key.
      // Keep it so the control can translate it. A key that is just the code
      // falls back to the master's name.
      const explicitLocaleKey = key && key !== o.code && key !== o.name;
      if (untranslated && !explicitLocaleKey && ds?.keepI18nKey !== true) {
        return { ...o, i18nKey: o.name || o.code };
      }
      return o;
    });

    // Alphabetical sort is opt-in via dataSource.sortOptions = "label".
    // Radios stay in config order (Yes/No must not be sorted).
    if (type !== "dropdown" || ds?.sortOptions !== "label") return labeled;
    const labelOf = (o) => {
      const key = o.i18nKey || o.name || o.code || "";
      const translated = key ? t(key) : "";
      if (!translated || translated === key) return String(o.name || o.code || key);
      return String(translated);
    };
    return [...labeled].sort((a, b) => labelOf(a).localeCompare(labelOf(b)));
  }, [type, name, field, fieldConfig, dropdownData, t, formData]);

  /**
   * Compiled validation.regex for stripping illegal characters on text input.
   * Null when the field has no regex rule. here is the regex rule for the field.
   *
   * @returns {RegExp|null}
   */
  const sanitizeRegex = useMemo(
    () =>
      validation.regex
        ? new RegExp(validation.regex.pattern, validation.regex.flags || "")
        : null,
    [validation.regex]
  );

  // ── Non-leaf: section header ─────────────────────────────────────────
  // ── Non-leaf: section header (respects visibleWhen) ──────────────────
  if (fieldConfig.type === "sectionHeader") {
    if (!isFieldVisible(fieldConfig, formData)) return null;
    const headerKey = fieldConfig.label?.code || fieldConfig.key;
    const headerDefault = fieldConfig.messages?.labelDefault;
    const headerTranslated = headerDefault
      ? t(headerKey, { defaultValue: headerDefault })
      : t(headerKey);
    const headerText =
      headerDefault && (!headerTranslated || headerTranslated === headerKey)
        ? headerDefault
        : headerTranslated;
    return (
      <h2 className="dynamic-form-field__section-header">
        {headerText}
      </h2>
    );
  }

  // ── Non-leaf: fieldArray — repeatable card rows with Add / Remove ────
  // this is the field array component which is used to repeat the field in a row
  if (fieldConfig.type === "fieldArray") {
    if (!isFieldVisible(fieldConfig, formData)) return null;

    const arrayName = getFieldArrayName(fieldConfig);
    const arrayLocked = fieldConfig.field?.disabledWhen
      ? evaluateFormRule(fieldConfig.field.disabledWhen, formData)
      : false;
    const controlsDisabled = isDisabled || arrayLocked;
    const minItems = Math.max(1, Number(fieldConfig.minItems) || 1);
    const children = fieldConfig.children || [];
    const rows = Array.isArray(formData[arrayName])
      ? formData[arrayName]
      : [createEmptyFieldArrayItem(children)];
    const addLabel = fieldConfig.addLabel || "CS_COMMON_ADD";
    const removeLabel = fieldConfig.removeLabel || "CS_COMMON_REMOVE";
    const itemLabel = fieldConfig.itemLabel || fieldConfig.key || arrayName;

    const commitRows = (nextRows, meta) => onChange(arrayName, nextRows, [], meta);

    const updateRowField = (rowIndex, childName, value, resetFields = []) => {
      const nextRows = rows.map((row, i) => {
        if (i !== rowIndex) return row;
        if (typeof enrichFieldArrayRow === "function") {
          return (
            enrichFieldArrayRow({
              childName,
              value,
              row,
              formData,
              resetFields,
              fieldConfig,
            }) || { ...row, [childName]: value }
          );
        }
        // this is the updated row date after the change is made in the field
        const updated = { ...row, [childName]: value };
        (resetFields || []).forEach((f) => {
          updated[f] = null;
        });
        return updated;
      });
      const clearErrorKeys = [
        `${arrayName}.${rowIndex}.${childName}`,
        ...(resetFields || []).map((f) => `${arrayName}.${rowIndex}.${f}`),
      ];
      commitRows(nextRows, { clearErrorKeys });
    };
    // this function is used to add a new row to the field array
    const addRow = () => {
      commitRows([...rows, createEmptyFieldArrayItem(children)]);
    };
    // this function is used to remove a row from the field array
    const removeRow = (rowIndex) => {
      if (rows.length <= minItems) return;
      commitRows(rows.filter((_, i) => i !== rowIndex), {
        refreshPrefixes: [arrayName],
      });
    };

    return (
      <div className="dynamic-form-field__field-array">
        {rows.map((row, rowIndex) => {
          const rowErrors = {};
          Object.keys(errors || {}).forEach((key) => {
            const prefix = `${arrayName}.${rowIndex}.`;
            if (key.startsWith(prefix) && errors[key]) {
              rowErrors[key.slice(prefix.length)] = errors[key];
            }
          });

          return (
            <div
              key={`${arrayName}-${rowIndex}`}
              className="dynamic-form-field__field-array-card"
            >
              <div className="dynamic-form-field__field-array-card-header">
                <h3 className="dynamic-form-field__field-array-card-title">
                  {t(itemLabel)} {rowIndex + 1}
                </h3>
                {rows.length > minItems ? (
                  <button
                    type="button"
                    className="dynamic-form-field__field-array-remove"
                    onClick={() => removeRow(rowIndex)}
                    disabled={controlsDisabled}
                  >
                    {t(removeLabel)}
                  </button>
                ) : null}
              </div>
              <div className="dynamic-form-field__field-array-card-body">
                {children.map((child) => (
                  <DynamicFormField
                    key={
                      child?.field?.type === "radio"
                        ? `${child.key}-${rowIndex}-${row?.[child?.field?.name] ?? ""}`
                        : `${child.key}-${rowIndex}`
                    }
                    fieldConfig={child}
                    formData={{ ...(formData || {}), ...(row || {}) }}
                    onChange={(name, value, resetFields) =>
                      updateRowField(rowIndex, name, value, resetFields)
                    }
                    onFieldBlur={onFieldBlur}
                    errors={rowErrors}
                    dropdownData={dropdownData}
                    t={t}
                    isDisabled={
                      controlsDisabled && child?.field?.enabledWhenArrayLocked !== true
                    }
                    onFileUpload={onFileUpload}
                    onFieldSearch={onFieldSearch}
                    isFieldSearching={isFieldSearching}
                    searchPanel={searchPanel}
                    onSelectSearchResult={onSelectSearchResult}
                    onCreateNewFromSearch={onCreateNewFromSearch}
                    enrichFieldArrayRow={enrichFieldArrayRow}
                    inputName={`${arrayName}.${rowIndex}.${child?.field?.name || child.key}`}
                  />
                ))}
              </div>
            </div>
          );
        })}
        <button
          type="button"
          className="dynamic-form-field__field-array-add"
          onClick={addRow}
          disabled={controlsDisabled}
        >
          {t(addLabel)}
        </button>
      </div>
    );
  }

  // ── Non-leaf: group of children in a row (respects visibleWhen) ──────
  if (fieldConfig.type === "group") {
    if (!isFieldVisible(fieldConfig, formData)) return null;
    return (
      <div className="dynamic-form-field__group">
        <FieldLabel
          text={t(fieldConfig.label?.code || fieldConfig.key)}
          unit={fieldConfig.label?.unit}
        />
        <div className="dynamic-form-field__group-row">
          {(fieldConfig.children || []).map((child) => (
            <DynamicFormField
              key={child.key}
              fieldConfig={child}
              formData={formData}
              onChange={onChange}
              onFieldBlur={onFieldBlur}
              errors={errors}
              dropdownData={dropdownData}
              t={t}
              isDisabled={isDisabled}
              onFileUpload={onFileUpload}
              onFieldSearch={onFieldSearch}
              isFieldSearching={isFieldSearching}
              searchPanel={searchPanel}
              onSelectSearchResult={onSelectSearchResult}
              onCreateNewFromSearch={onCreateNewFromSearch}
            />
          ))}
        </div>
      </div>
    );
  }

  // ── Leaf field: require field def + visibility ───────────────────────
  if (!field) return null;
  if (!isFieldVisible(fieldConfig, formData)) return null;

  const value = formData[name];
  const hasError = errors[name];
  const errorRule =
    typeof errors?.[name] === "string" && errors[name] ? errors[name] : "error";
  const errorKey = messages[errorRule] || messages.error || "FIELD_REQUIRED";
  const errorDefault =
    messages[`${errorRule}Default`] || messages.errorDefault || messages.labelDefault;
  const errorTranslated = errorDefault
    ? t(errorKey, { defaultValue: errorDefault })
    : t(errorKey);
  const errorMsgBase =
    errorDefault && (!errorTranslated || errorTranslated === errorKey)
      ? errorDefault
      : errorTranslated;
  const errorMsg = (() => {
    if (errorRule !== "min" && errorRule !== "max") return errorMsgBase;
    const loField = validation?.min?.fromField;
    const hiField = validation?.max?.fromField;
    const lo = loField != null ? formData?.[loField] : null;
    const hi = hiField != null ? formData?.[hiField] : null;
    if (lo == null && hi == null) return errorMsgBase;
    return `${errorMsgBase} ${lo ?? ""} - ${hi ?? ""}`.trim();
  })();
  const labelKey = resolveFieldLabelKey(fieldConfig, formData);
  const labelDefault = messages.labelDefault;
  const translatedLabel = labelDefault
    ? t(labelKey, { defaultValue: labelDefault })
    : t(labelKey);
  const labelText =
    labelDefault && (!translatedLabel || translatedLabel === labelKey)
      ? labelDefault
      : translatedLabel;

  const requiredByWhen =
    validation.requiredWhen && evaluateFormRule(validation.requiredWhen, formData);
  const showRequired = Boolean(validation.required || requiredByWhen);

  let fieldDisabled = Boolean(isDisabled || validation.disabled || validation.readOnly);
  if (field.enabledWhen) {
    fieldDisabled = fieldDisabled || !evaluateFormRule(field.enabledWhen, formData);
  }
  if (field.disabledWhen) {
    fieldDisabled = fieldDisabled || evaluateFormRule(field.disabledWhen, formData);
  }

  // Blur key is `array.index.child` inside a repeated row, otherwise the field name.
  const blurErrorKey = inputName || name;
  const reportBlur = () => {
    if (fieldDisabled) return;
    onFieldBlur?.(blurErrorKey);
  };

  const dropdownResetFields =
    field.dataSource?.resetFields || field.resetFields || fieldConfig.resetFields || [];

  /** Duration compute fields may display as years when months > 12. */
  const isDurationField = field.computeFn === "calculateDuration";
  const durationMonths = isDurationField ? Number(value) : NaN;
  const showDurationAsYears = isDurationField && Number.isFinite(durationMonths) && durationMonths > 12;
  const textDisplayValue = isDurationField ? formatDurationDisplay(value) : value;
  const textUnit = showDurationAsYears ? undefined : unit;
  /** Lookup UI when MDMS marks the field with searchCard or searchButton. */
  const useSearchCard = Boolean(field.searchCard || field.searchButton);
  /** Text Search button instead of magnifying-glass icon when searchButton is set. */
  const showSearchTextButton = Boolean(field.searchButton);

  // ── dropdown ─────────────────────────────────────────────────────────
  if (type === "dropdown") {
    const isFieldDisabled = fieldDisabled || fieldConfig.key === "EST_CITY";
    /**
     * Safe translator for Digit Dropdown (never returns empty for a key).
     * @param {string} key
     * @returns {string}
     */
    const tSafe = (key) => (key ? t(key) || key : "");
    // Prefer the option object from the current list (stable reference by code).
    // String codes / stale objects otherwise leave Dropdown selectedVal blank.
    const selectedCode = optionCode(value);
    const selected =
      (selectedCode && options.find((o) => optionCode(o) === selectedCode)) ||
      (value && typeof value === "object" ? value : null);

    return (
      <LabelFieldPair>
        <FieldLabel text={labelText} required={showRequired} hasError={hasError} />
        <div className="field" data-field-error={hasError ? "true" : undefined}>
          <Dropdown
            placeholder={tSafe(placeholder || "")}
            selected={selected}
            option={options}
            optionKey="i18nKey"
            select={(val) =>
              onChange(
                name,
                enrichDropdownSelection(val, options),
                dropdownResetFields
              )
            }
            t={tSafe}
            disable={isFieldDisabled}
            optionCardStyles={field.optionCardStyles}
            onBlur={reportBlur}
          />
          <FieldError show={hasError} message={errorMsg} />
        </div>
      </LabelFieldPair>
    );
  }

  // ── radio ────────────────────────────────────────────────────────────
  if (type === "radio") {
    const radioDisabled = fieldDisabled;
    return (
      <LabelFieldPair>
        <FieldLabel text={labelText} required={showRequired} hasError={hasError} />
        <div className="field" data-field-error={hasError ? "true" : undefined}>
          <div
            key={String(value ?? "")}
            className="radio-wrap dynamic-form-field__radio-group"
            onBlur={(event) => {
              // Moving between radios in the same group is not a field blur.
              if (event.currentTarget.contains(event.relatedTarget)) return;
              reportBlur();
            }}
          >
            {options.map((opt, index) => {
              const selected = radioOptionMatches(value, opt);
              const key = opt.i18nKey || opt.label || opt.name || opt.code;
              const translated = t(key);
              const optionLabel =
                (!translated || translated === key) && (opt.name || opt.label)
                  ? opt.name || opt.label
                  : translated || key;
              const inputId = `${radioInputName}-${index}-${opt.code ?? ""}`.replace(
                /[^A-Za-z0-9_-]+/g,
                "-"
              );
              return (
                <div key={String(opt.code ?? key)}>
                  {/* The native control stays invisible. The dot is a real element so a locked radio still shows its choice. */}
                  <span
                    className={`radio-btn-wrap${
                      selected ? " radio-btn-wrap--selected" : ""
                    }${radioDisabled ? " radio-btn-wrap--disabled" : ""}`}
                  >
                    <input
                      id={inputId}
                      type="radio"
                      name={radioInputName}
                      value={opt.code}
                      checked={selected}
                      aria-disabled={radioDisabled || undefined}
                      tabIndex={radioDisabled ? -1 : undefined}
                      onClick={(event) => {
                        if (radioDisabled) event.preventDefault();
                      }}
                      onChange={() => {
                        if (radioDisabled) return;
                        onChange(name, opt.code, dropdownResetFields);
                      }}
                      className="radio-btn"
                    />
                    <span
                      className="radio-btn-checkmark"
                      style={{
                        position: "relative",
                        ...(selected ? { borderColor: "#a82227" } : {}),
                      }}
                    >
                      {selected ? <span className="radio-btn-dot" /> : null}
                    </span>
                  </span>
                  <label
                    htmlFor={radioDisabled ? undefined : inputId}
                    className={`dynamic-form-field__radio-label${
                      radioDisabled ? " dynamic-form-field__radio-label--disabled" : ""
                    }${selected ? " dynamic-form-field__radio-label--selected" : ""}`}
                  >
                    {optionLabel}
                  </label>
                </div>
              );
            })}
          </div>
          <FieldError show={hasError} message={errorMsg} />
        </div>
      </LabelFieldPair>
    );
  }

  // ── date ─────────────────────────────────────────────────────────────
  if (type === "date") {
    const dateBounds = resolveFieldBoundDates(field);
    return (
      <LabelFieldPair>
        <FieldLabel text={labelText} required={showRequired} hasError={hasError} />
        <div className="field" data-field-error={hasError ? "true" : undefined}>
          <DatePicker
            date={toInputDate(value)}
            disable={fieldDisabled}
            min={dateBounds.min}
            max={dateBounds.max}
            onChange={(d) => onChange(name, d)}
            onBlur={reportBlur}
          />
          <FieldError show={hasError} message={errorMsg} />
        </div>
      </LabelFieldPair>
    );
  }

  // ── file upload ──────────────────────────────────────────────────────
  if (type === "file") {
    const fileRef =
      typeof value === "string"
        ? value
        : value?.filestoreId || value?.fileStoreId || value?.documentuuid;
    const hasUploaded = Boolean(fileRef);
    const uploadedLabel =
      (typeof value === "object" && (value?.fileName || value?.name)) ||
      t("CS_ACTION_FILEUPLOADED");

    return (
      <LabelFieldPair>
        <FieldLabel text={labelText} required={showRequired} hasError={hasError} />
        <div className="field" data-field-error={hasError ? "true" : undefined}>
          <UploadFile
            id={name}
            accept={field.accept || ".png,.jpg,.jpeg,.pdf"}
            message={hasUploaded ? t("CS_ACTION_FILEUPLOADED") : t("CS_ACTION_NO_FILEUPLOADED")}
            file={hasUploaded ? { name: uploadedLabel } : undefined}
            onUpload={(e) => onFileUpload && onFileUpload(name, e.target.files[0])}
            onDelete={() => onChange(name, null)}
            onBlur={reportBlur}
          />
          <FieldError show={hasError} message={errorMsg} />
        </div>
      </LabelFieldPair>
    );
  }

  // ── lookup / search card (text + search affordances) ─────────────────
  if (useSearchCard) {
    const panelForField = searchPanel?.fieldName === name ? searchPanel : null;
    return (
      <LabelFieldPair>
        <FieldLabel text={labelText} required={showRequired} hasError={hasError} unit={textUnit} />
        <div className="field" data-field-error={hasError ? "true" : undefined}>
          <div
            className={`dynamic-form-field__lookup${
              showSearchTextButton ? " dynamic-form-field__lookup--with-button" : ""
            }`}
          >
            <div className="dynamic-form-field__lookup-input-container">
              <TextInput
                placeholder={(() => {
                  if (!field.placeholderDefault) return t(placeholder || "");
                  const translated = t(placeholder || "", {
                    defaultValue: field.placeholderDefault,
                  });
                  return !translated || translated === placeholder
                    ? field.placeholderDefault
                    : translated;
                })()}
                value={value || ""}
                onChange={(e) =>
                  applyTextInputChange(e, { name, sanitizeRegex, validation, onChange })
                }
                onBlur={reportBlur}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    onFieldSearch?.(name);
                  }
                }}
                disabled={fieldDisabled}
                readOnly={validation.readOnly}
                errorStyle={hasError}
              />
              {panelForField?.status === "matches" && Array.isArray(panelForField.matches) && (
                <div className="dynamic-form-field__suggest-box">
                  {panelForField.matches.map((match) => (
                    <button
                      key={match.estateNo}
                      type="button"
                      className="dynamic-form-field__suggest-item"
                      onClick={() => onSelectSearchResult?.(name, match)}
                    >
                      <span className="dynamic-form-field__suggest-no">
                        {match.label || match.estateNo}
                      </span>
                      {match.subtitle ? (
                        <span className="dynamic-form-field__suggest-sub">
                          {match.subtitle}
                        </span>
                      ) : null}
                    </button>
                  ))}
                </div>
              )}
              {panelForField?.status === "found" && (
                <div className="dynamic-form-field__result-card">
                  <div className="dynamic-form-field__result-row">
                    <span>{t(field.resultLabel || labelKey || "CS_COMMON_ID")}</span>
                    <span>{panelForField.estateNo}</span>
                  </div>
                  {(Array.isArray(field.resultRows) ? field.resultRows : []).map((row) => {
                    const raw = panelForField.prefill?.[row.key];
                    if (raw == null || raw === "") return null;
                    return (
                      <div className="dynamic-form-field__result-row" key={row.key}>
                        <span>{t(row.label || row.key)}</span>
                        <span>{t(String(raw))}</span>
                      </div>
                    );
                  })}
                  <button
                    type="button"
                    className="dynamic-form-field__select-button"
                    onClick={() => onSelectSearchResult?.(name)}
                  >
                    {t(field.selectLabel || "CS_COMMON_SELECT")}
                  </button>
                </div>
              )}
              {panelForField?.status === "notFound" && (
                <div className="dynamic-form-field__not-found">
                  <p className="dynamic-form-field__not-found-text">
                    {translateWithDefault(
                      t,
                      field.notFoundLabel || "CS_COMMON_NOT_FOUND",
                      field.notFoundLabelDefault
                    )}
                  </p>
                  <button
                    type="button"
                    className="dynamic-form-field__create-button"
                    onClick={() => onCreateNewFromSearch?.(name)}
                  >
                    {translateWithDefault(
                      t,
                      field.createNewLabel || "CS_COMMON_CREATE_NEW",
                      field.createNewLabelDefault
                    )}
                  </button>
                </div>
              )}
            </div>
            <button
              type="button"
              className={
                showSearchTextButton
                  ? "dynamic-form-field__lookup-btn"
                  : "dynamic-form-field__lookup-icon"
              }
              disabled={
                isDisabled ||
                validation.disabled ||
                isFieldSearching ||
                !String(value || "").trim()
              }
              onClick={() => onFieldSearch?.(name)}
              aria-label={t(field.searchButtonLabel || "CS_COMMON_SEARCH")}
            >
              {showSearchTextButton
                ? t(field.searchButtonLabel || "CS_COMMON_SEARCH")
                : <SearchIcon />}
            </button>
          </div>
          <FieldError show={hasError} message={errorMsg} />
        </div>
      </LabelFieldPair>
    );
  }

  // ── default: plain text input ────────────────────────────────────────
  // here is the value check here
  return (
    <LabelFieldPair>
      <FieldLabel text={labelText} required={showRequired} hasError={hasError} unit={textUnit} />
      <div className="field" data-field-error={hasError ? "true" : undefined}>
        <TextInput
          placeholder={t(placeholder || "")}
          value={textDisplayValue || ""}
          onChange={(e) =>
            applyTextInputChange(e, { name, sanitizeRegex, validation, onChange })
          }
          onBlur={reportBlur}
          disabled={fieldDisabled}
          readOnly={validation.readOnly}
          errorStyle={hasError}
        />
        <FieldError show={hasError} message={errorMsg} />
      </div>
    </LabelFieldPair>
  );
};

/**
 * Field names this component must watch for memo compares
 * (own value, visibleWhen deps, computeFrom deps, etc.).
 *
 * @param {object} fc - fieldConfig
 * @returns {string[]}
 */
const collectNames = (fc) => getFieldWatchNames(fc);

/**
 * Deep-ish equality for a single watched form value.
 * Primitives use ===; option objects compare code + rent/multiplier metadata
 * so enriched dropdown selections don't spuriously re-render.
 * Arrays / fieldArray rows compare nested values — otherwise `String([row])`
 * collapses every row to "[object Object]" and subtype dependsOn never refreshes.
 *
 * @param {*} a
 * @param {*} b
 * @returns {boolean}
 */
const watchValueEqual = (a, b) => {
  if (a === b) return true;
  if (Array.isArray(a) || Array.isArray(b)) {
    if (!Array.isArray(a) || !Array.isArray(b) || a.length !== b.length) return false;
    return a.every((item, i) => watchValueEqual(item, b[i]));
  }
  if (a && b && typeof a === "object" && typeof b === "object") {
    // Dropdown / radio option shape
    if ("code" in a || "code" in b) {
      if (optionCode(a) !== optionCode(b)) return false;
      const metaKeys = ["multiplier", "rentMultiplier", "cycleMultiplier", "rentLabelKey"];
      return metaKeys.every((key) => (a[key] ?? null) === (b[key] ?? null));
    }
    // fieldArray row (or other plain object): compare own keys
    const keys = new Set([...Object.keys(a), ...Object.keys(b)]);
    return [...keys].every((k) => watchValueEqual(a[k], b[k]));
  }
  return false;
};

/**
 * React.memo comparator for DynamicFormField.
 * Re-renders when stable props identity changes, or when any watched formData
 * / errors entry for this fieldConfig differs (via collectNames + watchValueEqual).
 * Ignores unrelated formData keys so sibling field updates don't cascade.
 *
 * @param {object} prev - Previous props.
 * @param {object} next - Next props.
 * @returns {boolean} True when props are equal (skip re-render).
 */
const areEqual = (prev, next) => {
  if (
    prev.fieldConfig !== next.fieldConfig ||
    prev.dropdownData !== next.dropdownData ||
    prev.t !== next.t ||
    prev.isDisabled !== next.isDisabled ||
    prev.onChange !== next.onChange ||
    prev.onFieldBlur !== next.onFieldBlur ||
    prev.onFileUpload !== next.onFileUpload ||
    prev.onFieldSearch !== next.onFieldSearch ||
    prev.isFieldSearching !== next.isFieldSearching ||
    prev.searchPanel !== next.searchPanel ||
    prev.onSelectSearchResult !== next.onSelectSearchResult ||
    prev.onCreateNewFromSearch !== next.onCreateNewFromSearch ||
    prev.enrichFieldArrayRow !== next.enrichFieldArrayRow ||
    prev.inputName !== next.inputName
  ) {
    return false;
  }
  const names = collectNames(next.fieldConfig);
  const valuesEqual = names.every((n) =>
    watchValueEqual(prev.formData[n], next.formData[n])
  );
  if (!valuesEqual) return false;
  // there is the value check here

  if (next.fieldConfig?.type === "fieldArray") {
    const arrayName = getFieldArrayName(next.fieldConfig);
    const prefix = `${arrayName}.`;
    const prevKeys = Object.keys(prev.errors || {}).filter((k) => k.startsWith(prefix));
    const nextKeys = Object.keys(next.errors || {}).filter((k) => k.startsWith(prefix));
    if (prevKeys.length !== nextKeys.length) return false;
    return nextKeys.every((k) => prev.errors[k] === next.errors[k]);
  }

  return names.every((n) => prev.errors[n] === next.errors[n]);
};

export default React.memo(DynamicFormField, areEqual);
