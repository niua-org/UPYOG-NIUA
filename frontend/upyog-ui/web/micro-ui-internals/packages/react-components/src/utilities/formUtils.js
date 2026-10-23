/**
 * formUtils.js
 *
 * Single source of truth for form-config walking, option shaping, prefill,
 * payload flattening, and small DOM/date helpers used by DynamicForm and
 * related wizard/check-page utilities.
 *
 * Configs come from MDMS (structure, order, master options) overlaid with
 * module-local JSON (compute bindings, validation, labels, prefill). Values
 * in the live form stay rich objects (`{ code, name, i18nKey, multiplier }`);
 * wizard session storage stores the flattened code-plus-metadata shape that
 * `buildPayload` produces and `rehydrateBillingCycleOption` reads back.
 *
 * Responsibilities
 * ----------------
 * 1. Walk and normalize MDMS-driven form configs: flatten groups and
 *    fieldArrays, sort by `order`, merge local overrides onto MDMS fields,
 *    and split a form into accordion panels.
 * 2. Shape dropdown and radio values into the canonical
 *    `{ code, name, value, i18nKey }` object DynamicFormField renders
 *    (`i18nKey` is the display label, never the raw code). Coerce gender
 *    and relationship radios that arrive as prefixed master codes.
 * 3. Resolve option codes, enrich a selection with MDMS metadata
 *    (multiplier, rentLabelKey, …), collapse dotted master codes into
 *    category / type / subtype lists, and derive billing-cycle rent multipliers.
 * 4. Evaluate config rules (`visibleWhen`, `requiredWhen`, `disabledWhen`,
 *    `enabledWhen`) and resolve dynamic i18n labels from current form values.
 * 5. Build initial form values from a raw API/edit asset (including repeating
 *    fieldArray rows) and flatten those values for wizard session storage.
 * 6. Parse and format dates for native date inputs, including financial-year
 *    windows, and scroll the first field error into view.
 *
 * Exports
 * -------
 * Config walking:
 *   - flattenFormConfig          Expand group / fieldArray children into a flat field list.
 *   - getFieldArrayName          formData key for a fieldArray (`field.name`, else key tail).
 *   - createEmptyFieldArrayItem  Blank row object from a fieldArray's children.
 *   - sortByOrder                Sort top-level items and group children by `order`.
 *   - findFieldConfig            Find a leaf field config by `field.name`.
 *   - mergeFormFieldConfigs      Overlay local field overrides onto an MDMS form.
 *   - buildAccordionSections     `body[]` (or sectionHeader split) → accordion panels.
 *   - getAccordionCellClass      CSS span class for one cell in the accordion grid.
 *   - collectSectionFieldNames / errorKeyBelongsTo / sectionHasFieldError / findAccordionSectionId
 *
 * Dropdown / option shaping:
 *   - toDropdownOption           Build the canonical option object from code + name.
 *   - resolveOption              Resolve a code string or object to a full option.
 *   - enrichDropdownSelection    Merge a selection with the matching MDMS/config option.
 *   - optionCode                 Normalize a value to an uppercase code string.
 *   - coerceRadioFieldValue      Strip prefixes and aliases down to a radio code.
 *   - radioOptionMatches         Whether a stored radio value selects a given option.
 *   - normalizeBillingCycleCode  Uppercase billing-cycle code helper.
 *   - resolveBillingCycleMultiplier  Rent multiplier from the billing-cycle option.
 *   - rehydrateBillingCycleOption    Rebuild a billing-cycle object from flat session data.
 *   - filterDependsOnOptions     Collapse dotted master codes into category/type/subtype options.
 *
 * Field metadata / visibility:
 *   - resolveFieldLabelKey       Resolve the i18n label key (supports `field.labelBy`).
 *   - getFieldWatchNames         Names whose changes should re-render a field.
 *   - isFieldVisible             Config-driven show/hide (`hidden` / `visibleWhen`).
 *   - evaluateFormRule           Shared evaluator for visible/required/disabled/enabled rules.
 *
 * Prefill / payload:
 *   - buildInitialData           Initial values from form config + raw asset.
 *   - buildPayload               Flatten formData for wizard session storage.
 *
 * Date / DOM helpers:
 *   - toDate                     Parse string / epoch / Date → Date | null.
 *   - toInputDate                Format a value as local yyyy-MM-dd for `<input type="date">`.
 *   - resolveConfigDate          Resolve a minDate / maxDate spec to yyyy-MM-dd.
 *   - financialYearBounds        Current FY start (1 April) and end (31 March).
 *   - resolveFieldDateBounds     min/max for a date field, clamped to the FY when asked.
 *   - scrollToFirstError         Scroll the first `[data-field-error="true"]` into view.
 *
 * NOTE: toDropdownOption / resolveOption used to exist here AND in
 * payloadUtils.js with conflicting shapes (i18nKey = name vs i18nKey = code).
 * This file is now the only implementation; payloadUtils re-exports it.
 *
 * @see DynamicForm
 * @see DynamicFormField
 * @see payloadUtils.buildApiPayload  Richer API mapping (apiFieldName, numeric casts, …).
 */

/* ── config walking ─────────────────────────────────────────────────── */

/**
 * Flattens a form config into a single list of leaf fieldConfigs,
 * expanding `group` and `fieldArray` children. Shared by DynamicForm,
 * useDynamicMDMS, validators — do NOT re-implement locally.
 *
 * Note: fieldArray children are expanded for MDMS / option discovery.
 * Row validation walks fieldArray configs separately via validateFields.
 *
 * @param {Array<object>} [formConfig=[]] Top-level form config array (may contain groups).
 * @returns {Array<object>} Flat list of leaf field config objects (groups removed).
 */
export const flattenFormConfig = (formConfig = []) =>
  formConfig.reduce((acc, fc) => {
    // Containers are replaced by their children; the group/fieldArray node itself is dropped.
    if (fc?.type === "group" || fc?.type === "fieldArray") {
      return [...acc, ...(fc.children || [])];
    }
    return [...acc, fc];
  }, []);

/**
 * Resolve the formData key for a fieldArray config.
 * Prefers `field.name` so the session key stays stable if the MDMS `key`
 * is renamed. Otherwise uses the last `_` segment of `key`, with only the
 * first character lowercased (`EST_OWNERS` → `oWNERS`). Defaults to `"items"`.
 *
 * @param {object} fieldConfig  fieldArray config (`field.name` and/or `key`).
 * @returns {string} Key under which the row array is stored on formData.
 */
export const getFieldArrayName = (fieldConfig) => {
  if (fieldConfig?.field?.name) return fieldConfig.field.name;
  const key = String(fieldConfig?.key || "items");
  const segment = key.includes("_") ? key.split("_").pop() : key;
  return segment.charAt(0).toLowerCase() + segment.slice(1);
};

/**
 * Build an empty row object for a fieldArray from its children field names.
 * Dropdowns and file inputs start as `null` (nothing selected / uploaded);
 * every other type starts as `""` so controlled inputs stay defined.
 *
 * @param {Array<object>} [children=[]] Leaf field configs inside the fieldArray.
 * @returns {object} Row keyed by each child's `field.name`.
 */
export const createEmptyFieldArrayItem = (children = []) => {
  const row = {};
  (children || []).forEach((child) => {
    const name = child?.field?.name;
    if (!name) return;
    const type = child.field.type;
    if (type === "dropdown" || type === "file") row[name] = null;
    else row[name] = "";
  });
  return row;
};

/**
 * Sorts top-level form items by `order`, and any group's / fieldArray's children too.
 * Returns a new array; does not mutate the input.
 *
 * @param {Array<object>} [formConfig=[]] Top-level form config array.
 * @returns {Array<object>} Sorted copy of formConfig (group children sorted in-place on copies).
 */
export const sortByOrder = (formConfig = []) => {
  const byOrder = (a, b) => (a.order ?? 0) - (b.order ?? 0);
  return [...formConfig].sort(byOrder).map((item) =>
    (item.type === "group" || item.type === "fieldArray") && Array.isArray(item.children)
      ? { ...item, children: [...item.children].sort(byOrder) }
      : item
  );
};

/* ── dropdown option shaping (canonical) ────────────────────────────── */

/**
 * Build the canonical dropdown option shape.
 * DynamicFormField renders `optionKey="i18nKey"`, so i18nKey must be the
 * DISPLAY label (name), never the raw code. `value` is kept for legacy
 * consumers that keyed on it.
 *
 * @param {string} code  Option code (stored value / API key).
 * @param {string} [name] Display label; falls back to `code` when absent.
 * @returns {{ code: string, name: string, value: string, i18nKey: string }}
 */
export const toDropdownOption = (code, name) => ({
  code,
  name: name || code,
  value: code,
  i18nKey: name || code,
});

/**
 * Resolve a raw code string or an already-hydrated option object into a
 * full option. A string is looked up in `options` by code; when nothing
 * matches, a synthetic option is built with `toDropdownOption` unless
 * `strict` is set.
 *
 * @param {string|object|null|undefined} codeOrObj  Raw code or option object.
 * @param {string} [nameHint]                       Fallback display name when synthesizing.
 * @param {Array<object>} [options=[]]              Available options for lookup.
 * @param {object} [resolveOptions]
 * @param {boolean} [resolveOptions.strict=false]
 *        When true, return null if the code is not in `options` instead of
 *        inventing an option. Used when a stale session code must not appear
 *        as a selectable value.
 * @returns {object|null} Resolved option object, or null when input is falsy
 *          (or when `strict` and the code is unknown).
 */
export const resolveOption = (codeOrObj, nameHint, options = [], { strict = false } = {}) => {
  if (!codeOrObj) return null;
  if (typeof codeOrObj === "object") {
    const enriched = enrichDropdownSelection(codeOrObj, options);
    if (!strict) return enriched;
    // Strict mode keeps the object only when its code exists in the current list.
    const code = optionCode(enriched);
    const matched = (options || []).some((o) => optionCode(o) === code);
    return matched ? enriched : null;
  }
  // optionCode uppercases both sides; the second find keeps a case-sensitive
  // match for masters whose codes are not stored in uppercase.
  const matched =
    options.find((o) => optionCode(o) === optionCode(codeOrObj)) ||
    options.find((o) => o.code === codeOrObj);
  if (matched) return matched;
  if (strict) return null;
  return toDropdownOption(codeOrObj, nameHint || codeOrObj);
};

/**
 * Merge a dropdown selection with the full MDMS/config option so metadata
 * such as multiplier, rentLabelKey, etc. is available on the selected value.
 *
 * @param {string|object|null|undefined} selected  Current selection (code or object).
 * @param {Array<object>} [options=[]]             Full option list from MDMS/config.
 * @returns {string|object|null|undefined} Enriched selection, or original when no match.
 */
export const enrichDropdownSelection = (selected, options = []) => {
  if (!selected) return selected;
  // Nothing to merge against — leave a string as a string and an object as an object.
  if (!Array.isArray(options) || options.length === 0) return selected;
  const code = optionCode(selected);
  if (!code) return selected;
  const matched = options.find((o) => optionCode(o) === code);
  if (!matched) {
    // Unknown string codes still become a displayable option; unknown objects stay as-is.
    return typeof selected === "object" ? selected : toDropdownOption(selected, selected);
  }
  if (typeof selected === "object") {
    // Master metadata (multiplier, rentLabelKey) fills gaps; fields already on
    // the selection win. The canonical code stays the matched option's code.
    return { ...matched, ...selected, code: matched.code || selected.code };
  }
  return matched;
};

/* ── shared field-value helpers ─────────────────────────────────────── */

/**
 * Normalize dropdown/radio values to an uppercase code string.
 * Objects with a `code` property use that; plain strings are trimmed and uppercased.
 *
 * @param {string|object|null|undefined} val  Field value or option object.
 * @returns {string} Uppercase code, or empty string when value is null/undefined/"".
 */
export const optionCode = (val) => {
  if (val === null || val === undefined || val === "") return "";
  if (typeof val === "object" && val.code) return String(val.code).trim().toUpperCase();
  return String(val).trim().toUpperCase();
};

/* ── radio value coercion ───────────────────────────────────────────── */

/**
 * Prefixes property / trade masters store in front of gender and relationship
 * codes (`COMMON_GENDER_MALE`, `PT_RELATION_FATHER`). Radios compare the bare code.
 */
const RADIO_VALUE_PREFIXES = [
  "PT_COMMON_GENDER_",
  "COMMON_GENDER_",
  "TL_GENDER_",
  "PT_FORM3_",
  "TL_RELATIONSHIP_",
  "COMMON_RELATION_",
  "PT_RELATION_",
];

/** Short or alternate tokens that must select the same radio as the canonical code. */
const RADIO_VALUE_ALIASES = {
  M: "MALE",
  MAN: "MALE",
  F: "FEMALE",
  WOMAN: "FEMALE",
  T: "TRANSGENDER",
  TG: "TRANSGENDER",
  O: "OTHERS",
  OTHER: "OTHERS",
  // Same-as-property-owner is stored as boolean true/"true" by the legacy flow.
  TRUE: "YES",
  Y: "YES",
  FALSE: "NO",
  N: "NO",
};

/**
 * Turn a property / session gender or relationship into the radio code.
 * Accepts plain strings, `{ code }`, `{ value }`, and prefixed keys such as
 * `COMMON_RELATION_FATHER` or `TL_GENDER_MALE` (last segment is the code). Masked privacy values become "".
 *
 * @param {string|object|null|undefined} value
 * @returns {string}
 */
const RADIO_KNOWN_CODES = ["FEMALE", "MALE", "TRANSGENDER", "OTHERS", "HUSBAND", "FATHER"];

/** First known radio code inside a master key, dotted code, or plain label. */
const knownRadioCode = (token) => {
  if (!token) return "";
  if (RADIO_KNOWN_CODES.includes(token)) return token;
  // FEMALE is listed before MALE so the longer word wins.
  const found = token.match(/FEMALE|MALE|TRANSGENDER|OTHERS|HUSBAND|FATHER/);
  return found ? found[0] : "";
};

export const coerceRadioFieldValue = (value) => {
  if (value == null || value === "") return "";
  if (Array.isArray(value)) return coerceRadioFieldValue(value[0]);
  if (typeof value === "object") {
    const nested =
      value.code ??
      value.value ??
      value.name ??
      value.i18nKey ??
      value.i18nkey ??
      value.label ??
      value.plainValue ??
      value.unmaskedValue;
    if (nested && typeof nested === "object") return coerceRadioFieldValue(nested);
    return coerceRadioFieldValue(nested);
  }
  let token = String(value)
    .trim()
    .toUpperCase()
    .replace(/[\u200B-\u200D\uFEFF]/g, "");
  // Masked privacy values ("****") and a stringified object are not selectable codes.
  if (!token || token.includes("*") || token === "[OBJECT OBJECT]") return "";
  RADIO_VALUE_PREFIXES.forEach((prefix) => {
    if (token.startsWith(prefix)) token = token.slice(prefix.length);
  });
  if (RADIO_VALUE_ALIASES[token]) return RADIO_VALUE_ALIASES[token];
  // Dotted and underscored master keys: GENDER.MALE, TL_GENDER_MALE, PT.RELATION.FATHER.
  // Slash is left intact so a guardian phrase such as S/O is not read as OTHERS.
  const parts = token.split(/[._\s-]+/).filter(Boolean);
  const tail = parts[parts.length - 1];
  if (tail && tail !== token && RADIO_VALUE_ALIASES[tail]) return RADIO_VALUE_ALIASES[tail];
  const known = knownRadioCode(token) || knownRadioCode(tail);
  if (known) return known;
  // Keep unrecognized codes (a module-specific relationship) so the radio can still match.
  return token;
};

/**
 * True when a stored radio value selects this option.
 * Compares code, name, and i18n key after coerceRadioFieldValue.
 *
 * @param {string|object|null|undefined} value
 * @param {object} option
 * @returns {boolean}
 */
export const radioOptionMatches = (value, option) => {
  const selected = coerceRadioFieldValue(value);
  if (!selected || !option) return false;
  // Any of the option's labels may be what the API stored; compare after the same coercion.
  return [option.code, option.value, option.name, option.i18nKey, option.label]
    .map((part) => coerceRadioFieldValue(part))
    .filter(Boolean)
    .includes(selected);
};

/* ── billing cycle ──────────────────────────────────────────────────── */

/**
 * Uppercase billing-cycle code from a dropdown value or plain string.
 * Thin wrapper around optionCode for billing-cycle-specific call sites.
 *
 * @param {string|object|null|undefined} val  Billing-cycle value or option.
 * @returns {string} Uppercase billing-cycle code, or empty string.
 */
export const normalizeBillingCycleCode = (val) => optionCode(val);

/** Legacy fallbacks when MDMS option metadata is missing (string codes only). */
const LEGACY_BILLING_CYCLE_MULTIPLIERS = {
  MONTHLY: 1,
  QUARTERLY: 3,
  YEARLY: 12,
};

/**
 * Rent multiplier from the selected billing-cycle option (MDMS-driven).
 * Option may carry `multiplier`, `rentMultiplier`, or `cycleMultiplier`.
 * Falls back to LEGACY_BILLING_CYCLE_MULTIPLIERS when MDMS metadata is absent.
 *
 * @param {string|object|null|undefined} billingCycle  Selected billing-cycle value.
 * @param {Array<object>} [options=[]]                   Billing-cycle option list from MDMS.
 * @returns {number} Positive rent multiplier (defaults to 1).
 */
export const resolveBillingCycleMultiplier = (billingCycle, options = []) => {
  const enriched = enrichDropdownSelection(billingCycle, options);
  if (enriched && typeof enriched === "object") {
    for (const key of ["multiplier", "rentMultiplier", "cycleMultiplier"]) {
      const num = Number(enriched[key]);
      if (Number.isFinite(num) && num > 0) return num;
    }
  }
  const cycle = normalizeBillingCycleCode(enriched ?? billingCycle);
  const fromOptions = options.find((o) => optionCode(o) === cycle);
  if (fromOptions) {
    for (const key of ["multiplier", "rentMultiplier", "cycleMultiplier"]) {
      const num = Number(fromOptions[key]);
      if (Number.isFinite(num) && num > 0) return num;
    }
  }
  return LEGACY_BILLING_CYCLE_MULTIPLIERS[cycle] ?? 1;
};

/* ── dynamic labels ─────────────────────────────────────────────────── */

/**
 * Resolve an i18n label key from a `labelBy` rule and current form values.
 * Prefers the selected option's `optionKey` property, then a flattened
 * meta key on formValues, then the legacy `map` lookup by code, then `defaultKey`.
 *
 * @param {object} labelBy                    Label resolution rule from field config.
 * @param {string} labelBy.field              Form field name whose value drives the label.
 * @param {string} [labelBy.optionKey]        Property on the selected option (e.g. rentLabelKey).
 * @param {object} [labelBy.map]              Legacy code → i18n key map.
 * @param {string} [labelBy.defaultKey]       Fallback i18n key.
 * @param {object} [formValues={}]            Current form data.
 * @returns {string|undefined} Resolved i18n label key, or undefined when none match.
 */
const resolveLabelByCode = (labelBy, formValues = {}) => {
  const selected = formValues[labelBy.field];
  // Preferred: the live option object already carries the i18n key (e.g. rentLabelKey).
  if (labelBy.optionKey && selected && typeof selected === "object") {
    const fromOption = selected[labelBy.optionKey];
    if (fromOption) return fromOption;
  }
  // After buildPayload the option is a string code; metadata lives on a sibling key
  // such as billingCycle + rentLabelKey → billingCycleRentLabelKey.
  if (labelBy.optionKey) {
    const flatMetaKey = `${labelBy.field}${labelBy.optionKey.charAt(0).toUpperCase()}${labelBy.optionKey.slice(1)}`;
    if (formValues[flatMetaKey]) return formValues[flatMetaKey];
  }
  const raw = optionCode(selected);
  return labelBy.map?.[raw] || labelBy.defaultKey;
};

/**
 * Resolve the i18n label key for a field. Supports optional `field.labelBy`:
 *   labelBy: {
 *     field: "billingCycle",
 *     optionKey: "rentLabelKey",   // preferred — read from MDMS option
 *     map: { MONTHLY: "EST_..." },  // optional legacy fallback
 *     defaultKey: "EST_...",
 *   }
 *
 * When no labelBy is configured, returns summaryLabel or the field's config key.
 *
 * @param {object} fieldConfig              Leaf field config object.
 * @param {object} [formValues={}]          Current form data for labelBy resolution.
 * @returns {string|undefined} i18n label key for the field heading/label.
 */
export const resolveFieldLabelKey = (fieldConfig, formValues = {}) => {
  const labelBy = fieldConfig?.field?.labelBy;
  if (!labelBy) {
    return (
      fieldConfig?.summaryLabel ||
      fieldConfig?.field?.code ||
      fieldConfig?.key
    );
  }

  return resolveLabelByCode(labelBy, formValues);
};

/**
 * Find a leaf field config by `field.name` within a (possibly grouped) form config.
 *
 * @param {Array<object>} [formConfig=[]] Form config array (groups are flattened internally).
 * @param {string} fieldName              Target `field.name` value.
 * @returns {object|undefined} Matching leaf field config, or undefined.
 */
export const findFieldConfig = (formConfig = [], fieldName) =>
  flattenFormConfig(formConfig).find((fc) => fc.field?.name === fieldName);

/* ── MDMS / local field merge ──────────────────────────────────────── */

/**
 * Merge a single local field override onto its MDMS counterpart.
 * Local wins for compute bindings (computeFrom, computeFn, labelBy, prefillFrom),
 * validation/messages, apiFieldName, submitKey, excludeFromPayload, and hidden.
 * `dataSource` is deep-merged so a module can add label prefixes without
 * replacing the MDMS master, filter, and hierarchy.
 * MDMS options replace local options when MDMS provides a non-empty options array.
 *
 * @param {object} local  Local field config override.
 * @param {object} mdms   MDMS field config baseline.
 * @returns {object} Merged field config object.
 */
const mergeFormField = (local, mdms) => {
  const mergedField = { ...local.field, ...mdms.field };
  // Local bindings for compute/label/prefill must survive MDMS field overrides.
  ["name", "code", "computeFrom", "computeFn", "labelBy", "prefillFrom", "numeric", "unit", "defaultValue", "minDate", "maxDate", "withinFinancialYear", "createNewPath", "searchButton", "searchCard", "placeholder", "placeholderDefault", "resultLabel", "selectLabel", "notFoundLabel", "notFoundLabelDefault", "createNewLabel", "searchButtonLabel", "resultRows"].forEach((key) => {
    if (local.field?.[key] != null) mergedField[key] = local.field[key];
  });
  if (local.field?.dataSource || mdms.field?.dataSource) {
    mergedField.dataSource = {
      ...(mdms.field?.dataSource || {}),
      ...(local.field?.dataSource || {}),
    };
  }

  // Local validation/messages win — MDMS often over-escapes regex patterns (e.g. \\\\.).
  const merged = {
    ...local,
    ...mdms,
    field: mergedField,
    validation: { ...(mdms.validation || {}), ...(local.validation || {}) },
    messages: { ...(mdms.messages || {}), ...(local.messages || {}) },
  };
  ["apiFieldName", "submitKey", "excludeFromPayload", "hidden", "summaryLabel"].forEach((key) => {
    if (local[key] != null) merged[key] = local[key];
  });
  if (Array.isArray(mdms.options) && mdms.options.length > 0) {
    merged.options = mdms.options;
  }
  return merged;
};

/**
 * Merge local field overrides onto an MDMS-driven form.
 * MDMS defines structure/order/options; local wins for compute, validation, prefill, labelBy.
 * When either side is empty, returns the other side sorted by order.
 *
 * @param {Array<object>} [localForm=[]]  Module-local form config overrides.
 * @param {Array<object>} [mdmsForm=[]]   MDMS-driven form config baseline.
 * @returns {Array<object>} Merged, order-sorted form config array.
 */
export const mergeFormFieldConfigs = (localForm = [], mdmsForm = []) => {
  if (!Array.isArray(mdmsForm) || mdmsForm.length === 0) {
    return sortByOrder(localForm);
  }
  if (!Array.isArray(localForm) || localForm.length === 0) {
    return sortByOrder(mdmsForm);
  }

  const localByName = new Map();
  const localByKey = new Map();
  const nameCounts = new Map();
  flattenFormConfig(localForm).forEach((fc) => {
    if (fc?.field?.name) {
      localByName.set(fc.field.name, fc);
      nameCounts.set(fc.field.name, (nameCounts.get(fc.field.name) || 0) + 1);
    }
    if (fc?.key) localByKey.set(fc.key, fc);
  });

  // Match on config `key` first. Fall back to `field.name` only when that name
  // appears once locally — two "gender" radios in different groups must not
  // pick up each other's override.
  const resolveLocal = (item) => {
    if (item?.key && localByKey.has(item.key)) return localByKey.get(item.key);
    const name = item?.field?.name;
    if (name && nameCounts.get(name) === 1) return localByName.get(name);
    return null;
  };

  const overlayMdmsItem = (mdmsItem) => {
    if (mdmsItem?.type === "group" || mdmsItem?.type === "fieldArray") {
      return {
        ...mdmsItem,
        children: (mdmsItem.children || []).map((child) => {
          const localChild = resolveLocal(child);
          return localChild ? mergeFormField(localChild, child) : child;
        }),
      };
    }
    if (mdmsItem?.type === "sectionHeader") {
      const localHeader = localByKey.get(mdmsItem.key);
      return localHeader ? { ...mdmsItem, ...localHeader, type: "sectionHeader" } : mdmsItem;
    }
    const localItem = resolveLocal(mdmsItem);
    return localItem ? mergeFormField(localItem, mdmsItem) : mdmsItem;
  };

  return sortByOrder(mdmsForm.map(overlayMdmsItem));
};

/* ── accordion sections ─────────────────────────────────────────────── */

/**
 * Split a flat form[] into accordion panels on `type: "sectionHeader"`.
 * The header becomes the panel title and is not repeated inside the body.
 *
 * @param {Array<object>} [form=[]]
 * @returns {Array<object>}
 */
const splitFormBySectionHeaders = (form = []) => {
  const sections = [];
  let current = null;

  const startSection = (meta) => {
    if (current && current.form.length > 0) sections.push(current);
    current = {
      id: meta.id,
      titleKey: meta.titleKey,
      titleDefault: meta.titleDefault,
      hintDefault: meta.hintDefault,
      form: [],
    };
  };

  form.forEach((item, index) => {
    if (item?.type === "sectionHeader") {
      startSection({
        id: String(item.key || `section-${index + 1}`),
        titleKey: item.label?.code || item.key,
        titleDefault: item.messages?.labelDefault,
        hintDefault: item.messages?.hintDefault,
      });
      return;
    }
    if (!current) {
      // Fields that sit above the first sectionHeader still need a panel.
      startSection({
        id: "section-1",
        titleKey: "COMMON_FORM",
        titleDefault: null,
        hintDefault: null,
      });
    }
    current.form.push(item);
  });

  if (current && current.form.length > 0) sections.push(current);
  return sections;
};

/**
 * Overlay AccordionNewApplication `lists` metadata onto fieldArray items.
 *
 * @param {Array<object>} form
 * @param {object} [lists={}]
 * @returns {Array<object>}
 */
const applyAccordionListMeta = (form = [], lists = {}) => {
  if (!lists || typeof lists !== "object") return form;
  return form.map((item) => {
    if (item?.type !== "fieldArray") return item;
    const meta = lists[item.key];
    if (!meta) return item;
    return {
      ...item,
      minItems: meta.minItems ?? item.minItems,
      maxItems: meta.maxItems ?? item.maxItems,
      addLabel: meta.addLabel || item.addLabel,
      removeLabel: meta.removeLabel || item.removeLabel,
      itemLabel: meta.itemLabelDefault || meta.itemLabel || item.itemLabel,
      // The field config's own rule wins; lists metadata only fills a gap.
      visibleWhen: item.visibleWhen || meta.visibleWhen,
    };
  });
};

/**
 * CSS cell class for the accordion dense grid.
 * Headers, groups, repeating rows, and file uploads take the full row.
 * Radios and search widgets span two columns. Everything else is one cell.
 *
 * @param {object} [fieldConfig={}] Leaf, group, or fieldArray config.
 * @returns {string} `dynamic-form-accordion__cell`, plus `--full` or `--span-2`.
 */
export const getAccordionCellClass = (fieldConfig = {}) => {
  const base = "dynamic-form-accordion__cell";
  if (
    fieldConfig.type === "sectionHeader" ||
    fieldConfig.type === "fieldArray" ||
    fieldConfig.type === "group" ||
    fieldConfig.field?.type === "file"
  ) {
    return `${base} ${base}--full`;
  }
  if (
    fieldConfig.field?.type === "radio" ||
    fieldConfig.field?.searchCard ||
    fieldConfig.field?.searchButton
  ) {
    return `${base} ${base}--span-2`;
  }
  return base;
};

/**
 * Build accordion panels from MDMS / route JSON.
 * Prefers `config.body[]` (each item is a panel with its own `form[]`).
 * Falls back to splitting a flat `config.form[]` on `sectionHeader` entries.
 *
 * @param {object}        [config={}]     Full MDMS entry (`head`, `body`, `form`, `navigation`).
 * @param {Array<object>} [localForm=[]]  Field overlays (same shape as mergeFormFieldConfigs local).
 * @returns {Array<{ id: string, titleKey: string, titleDefault?: string, hintDefault?: string, form: object[] }>}
 */
export const buildAccordionSections = (config = {}, localForm = []) => {
  const overlay = (form) =>
    applyAccordionListMeta(mergeFormFieldConfigs(localForm, form || []), config.lists);
  const body = Array.isArray(config.body) ? config.body : [];
  const fromBody = body
    .filter((step) => {
      // Preview and review steps are not editable accordion panels.
      if (step?.isPreview || step?.type === "review") return false;
      return Array.isArray(step?.form) && step.form.length > 0;
    })
    .sort((a, b) => (a.order ?? 0) - (b.order ?? 0))
    .map((step) => ({
      id: String(step.key || step.route || step.i18nKey || "section"),
      titleKey: step.i18nKey || step.texts?.header || step.key,
      titleDefault: step.labelDefault || step.shortLabel,
      hintKey: step.hintKey,
      hintDefault: step.hintDefault,
      isMandatory: step.isMandatory,
      form: overlay(step.form),
    }));

  if (fromBody.length > 0) return fromBody;

  return splitFormBySectionHeaders(overlay(config.form || []));
};

/**
 * Field / fieldArray names owned by a section form (for error highlighting).
 *
 * @param {Array<object>} [form=[]]
 * @returns {Set<string>}
 */
export const collectSectionFieldNames = (form = []) => {
  const names = new Set();
  flattenFormConfig(form).forEach((fc) => {
    if (fc?.field?.name) names.add(fc.field.name);
  });
  (form || []).forEach((item) => {
    if (item?.type === "fieldArray") {
      const arrayName = getFieldArrayName(item);
      if (arrayName) names.add(arrayName);
    }
  });
  return names;
};

/**
 * True when an error key is the field itself or a nested key under it.
 * Field-array errors are stored as `owners.0.name`, so the array name matches.
 *
 * @param {string} key
 * @param {Set<string>} names
 * @returns {boolean}
 */
export const errorKeyBelongsTo = (key, names) => {
  if (!key || !names?.size) return false;
  if (names.has(key)) return true;
  for (const name of names) {
    if (key.startsWith(`${name}.`)) return true;
  }
  return false;
};

/**
 * True when `errors` includes a key belonging to this section's fields.
 *
 * @param {Array<object>} [form=[]]
 * @param {object}        [errors={}]
 * @returns {boolean}
 */
export const sectionHasFieldError = (form, errors = {}) => {
  const names = collectSectionFieldNames(form);
  return Object.entries(errors || {}).some(
    ([key, value]) => Boolean(value) && errorKeyBelongsTo(key, names)
  );
};

/**
 * First accordion section id that currently has a field error.
 *
 * @param {Array<object>} [sections=[]]
 * @param {object}        [errors={}]
 * @returns {string|null}
 */
export const findAccordionSectionId = (sections = [], errors = {}) => {
  const hit = (sections || []).find((s) => sectionHasFieldError(s.form, errors));
  return hit?.id || null;
};

/**
 * Rehydrate a flattened billing-cycle code using session metadata or form options.
 * When flatData already holds an object, returns it unchanged. Otherwise resolves
 * from routeConfig options or reconstructs from flattened multiplier/rentLabelKey keys.
 *
 * @param {object} [flatData={}]     Flattened session/wizard data (may contain billingCycle code).
 * @param {object} [routeConfig={}]  Route config with `form` array for option lookup.
 * @returns {string|object|null|undefined} Rehydrated billing-cycle option object or raw code.
 */
export const rehydrateBillingCycleOption = (flatData = {}, routeConfig = {}) => {
  const raw = flatData.billingCycle;
  if (raw && typeof raw === "object") return raw;

  const billingField = findFieldConfig(routeConfig?.form, "billingCycle");
  const fromOptions = resolveOption(
    raw,
    flatData.billingCycleName,
    billingField?.options || []
  );
  if (fromOptions && typeof fromOptions === "object") return fromOptions;

  if (!raw) return raw;

  // Session storage kept the multiplier / label beside the bare code
  // (billingCycleMultiplier, billingCycleRentLabelKey). Rebuild the object
  // when the option list itself is unavailable.
  const multiplier =
    flatData.billingCycleMultiplier ??
    flatData.billingCycleRentMultiplier ??
    flatData.billingCycleCycleMultiplier;
  const rentLabelKey = flatData.billingCycleRentLabelKey;
  if (multiplier != null || rentLabelKey) {
    return { code: raw, multiplier, rentLabelKey };
  }
  return raw;
};

/* ── visibility and form rules ──────────────────────────────────────── */

/**
 * Collect field names referenced by a visibleWhen rule (including nested and/or).
 * @param {object} rule
 * @param {Set<string>} [out]
 * @returns {Set<string>}
 */
const collectVisibleWhenFields = (rule, out = new Set()) => {
  if (!rule || typeof rule !== "object") return out;
  if (rule.field) out.add(rule.field);
  (rule.and || []).forEach((r) => collectVisibleWhenFields(r, out));
  (rule.or || []).forEach((r) => collectVisibleWhenFields(r, out));
  return out;
};

/**
 * Collect form field names whose value changes should trigger a re-render
 * of the given field (value, dynamic label, compute dependencies, visibility).
 * Recurses into group children.
 *
 * @param {object} fieldConfig  Field or group config object.
 * @returns {Array<string>} Unique list of watched form field names.
 */
export const getFieldWatchNames = (fieldConfig) => {
  if (fieldConfig?.type === "group") {
    return (fieldConfig.children || []).flatMap(getFieldWatchNames);
  }
  if (fieldConfig?.type === "fieldArray") {
    const names = new Set([getFieldArrayName(fieldConfig)]);
    collectVisibleWhenFields(fieldConfig.visibleWhen, names);
    collectVisibleWhenFields(fieldConfig.field?.disabledWhen, names);
    collectVisibleWhenFields(fieldConfig.disabledWhen, names);
    return [...names];
  }
  const field = fieldConfig?.field;
  if (!field) {
    // sectionHeader and similar nodes only re-render when their visibility rule changes.
    const names = new Set();
    collectVisibleWhenFields(fieldConfig?.visibleWhen, names);
    return [...names];
  }

  const names = new Set([field.name]);
  // Also watch the fields this one derives from: dynamic label, computed value,
  // prefill source, cascading dropdown parent, and show/require/enable rules.
  if (field.labelBy?.field) names.add(field.labelBy.field);
  (field.computeFrom || []).forEach((n) => names.add(n));
  if (field.prefillFrom) names.add(field.prefillFrom);
  if (field.dataSource?.dependsOn) names.add(field.dataSource.dependsOn);
  collectVisibleWhenFields(fieldConfig.visibleWhen, names);
  collectVisibleWhenFields(fieldConfig.validation?.requiredWhen, names);
  collectVisibleWhenFields(field.disabledWhen, names);
  collectVisibleWhenFields(field.enabledWhen, names);
  return [...names];
};

/**
 * Config-driven show/hide for a field.
 * Supports `hidden: true`, or `visibleWhen`:
 *   - `{ field, equals }`
 *   - `{ field, in: [...] }`
 *   - `{ field, notEmpty: true }` — show when the referenced value is non-empty
 *   - `{ and: [ rule, ... ] }` — all rules must pass
 *   - `{ or: [ rule, ... ] }` — any rule may pass
 * Dropdown/object values are compared by uppercase `code`; plain strings are uppercased.
 *
 * @param {object} fieldConfig         Leaf or group field config.
 * @param {object} [formData={}]       Current form values.
 * @returns {boolean} True when the field should be rendered.
 */
export const isFieldVisible = (fieldConfig, formData = {}) => {
  if (fieldConfig?.hidden === true) return false;

  const rule = fieldConfig?.visibleWhen;
  if (!rule) return true;

  return evaluateFormRule(rule, formData);
};

/**
 * Evaluate a form rule (`visibleWhen` / `requiredWhen` / `disabledWhen` / `enabledWhen`).
 * Supports nested and/or, equals, in, notEmpty.
 *
 * @param {object} rule
 * @param {object} formData
 * @returns {boolean}
 */
export const evaluateFormRule = (rule, formData = {}) => {
  if (!rule || typeof rule !== "object") return true;

  if (Array.isArray(rule.and)) {
    return rule.and.every((r) => evaluateFormRule(r, formData));
  }
  if (Array.isArray(rule.or)) {
    return rule.or.some((r) => evaluateFormRule(r, formData));
  }
  if (!rule.field) return true;

  const raw = formData[rule.field];
  // Dropdown objects compare by code; everything else is trimmed and uppercased
  // so "male" and "MALE" satisfy the same equals / in rule.
  const current =
    raw && typeof raw === "object" && raw.code != null
      ? String(raw.code).trim().toUpperCase()
      : String(raw ?? "").trim().toUpperCase();

  if (rule.notEmpty === true) {
    return current.length > 0;
  }
  if (rule.notEmpty === false) {
    return current.length === 0;
  }
  if (rule.equals !== undefined) {
    return current === String(rule.equals).trim().toUpperCase();
  }
  if (Array.isArray(rule.in)) {
    return rule.in.map((v) => String(v).trim().toUpperCase()).includes(current);
  }
  return true;
};

/**
 * @deprecated Use evaluateFormRule
 */
const evaluateVisibleWhenRule = evaluateFormRule;

/* ── hierarchical master options ────────────────────────────────────── */

/**
 * Collapse dotted master codes into category / type / subtype options.
 * Label keys come from the field dataSource:
 * - `i18nKeyPrefix` for a single segment (`PREFIX_<segment>`)
 * - `leafI18nPrefix` for the full leaf code (`PREFIX_<code>`)
 * With no prefix, the segment or the master's own key is used.
 *
 * @param {Array<object>} options - Raw MDMS rows
 * @param {object} [dataSource={}]
 * @param {object} [formData={}]
 * @returns {Array<object>}
 */
export const filterDependsOnOptions = (options = [], dataSource = {}, formData = {}) => {
  const hierarchy = dataSource.hierarchy || dataSource.optionLevel;
  const list = Array.isArray(options) ? options : [];
  const segmentKey = (code) => {
    const segment = String(code).replace(/\./g, "_");
    const prefix = dataSource.i18nKeyPrefix;
    return prefix ? `${prefix}_${segment}` : segment;
  };
  const leafKey = (item) => {
    const existing = item?.i18nKey ? String(item.i18nKey) : "";
    const prefix = dataSource.leafI18nPrefix;
    if (!prefix) return existing || item?.name || item?.code;
    const withPrefix = `${prefix}_`;
    return existing.startsWith(withPrefix) ? existing : `${withPrefix}${item.code}`;
  };

  // MDMS spells this flag "customiztionRequired" (missing "a"); it means category mode.
  if (hierarchy === "category" || dataSource.customiztionRequired) {
    const seen = new Set();
    const out = [];
    list.forEach((item) => {
      const code = String(item?.code || "").split(".")[0];
      const seenKey = code.toUpperCase();
      if (!code || seen.has(seenKey)) return;
      seen.add(seenKey);
      out.push({
        ...item,
        code,
        name: code,
        i18nKey: segmentKey(code),
      });
    });
    return out;
  }

  const parentName = dataSource.dependsOn;
  if (!parentName) return list;

  const parentCode = optionCode(formData[parentName]);
  if (!parentCode) return [];

  // Type level: keep the second segment of CATEGORY.TYPE.SUBTYPE rows whose
  // category matches the parent, de-duplicated.
  if (hierarchy === "type" || dataSource.dependsOnSegment === 0) {
    const seen = new Set();
    const out = [];
    list.forEach((item) => {
      const parts = String(item?.code || "").split(".");
      if (parts[0]?.toUpperCase() !== parentCode || !parts[1]) return;
      const code = parts[1];
      const seenKey = code.toUpperCase();
      if (seen.has(seenKey)) return;
      seen.add(seenKey);
      out.push({
        ...item,
        code,
        name: code,
        i18nKey: segmentKey(code),
      });
    });
    return out;
  }

  if (hierarchy === "subtype" || dataSource.dependsOnSegment === 1) {
    // Parent may be the type segment or a dotted path — normalize.
    const parentParts = parentCode.split(".").filter(Boolean);
    const typeCode =
      parentParts.length >= 2 ? parentParts[1] : parentParts[0] || parentCode;

    return list
      .filter((item) => {
        const parts = String(item?.code || "")
          .split(".")
          .filter(Boolean);
        // Leaf subtype codes are CATEGORY.TYPE.SUBTYPE
        return parts.length >= 3 && parts[1]?.toUpperCase() === typeCode;
      })
      .map((item) => ({
        ...item,
        code: item.code,
        name: item.name || item.code,
        i18nKey: leafKey(item),
      }));
  }

  // Generic: keep options whose code starts with parent (prefix match).
  const prefix = `${parentCode}.`;
  return list.filter((item) => {
    const code = String(item?.code || "").toUpperCase();
    return code === parentCode || code.startsWith(prefix);
  });
};

/**
 * Read a prefill path. A dotted path such as `property.address.doorNo`
 * walks the object; a literal key is used when present.
 *
 * @param {object} source
 * @param {string} path
 * @returns {*}
 */
const readPrefillPath = (source, path) => {
  if (!source || typeof source !== "object" || path == null || path === "") return undefined;
  // A literal key that exists on the source wins, so "address.city" stored as
  // one property is not split into address → city.
  if (Object.prototype.hasOwnProperty.call(source, path) && source[path] !== undefined) {
    return source[path];
  }
  if (!String(path).includes(".")) return undefined;
  return String(path)
    .split(".")
    .reduce((acc, key) => {
      // Refuse prototype-chain keys in a config-supplied path.
      if (acc == null || key === "__proto__" || key === "constructor" || key === "prototype") {
        return undefined;
      }
      return acc[key];
    }, source);
};

/* ── prefill ────────────────────────────────────────────────────────── */

/**
 * Walk any module's form config (including groups) and build initialData
 * generically — no hardcoded field names.
 *
 * Handles dropdown (with tenantId defaultValueSource), radio, file, date,
 * prefillFrom, and defaultValue (including `"today"` for date fields).
 *
 * @param {Array<object>} [formConfig=[]]   Form config array.
 * @param {object} [rawAsset={}]            Raw API/edit asset keyed by field name.
 * @param {object} [dropdownData={}]        Hook-built dropdown options keyed by field name.
 * @param {string} [tenantId]               Current tenant id (for tenantId dataSource fields).
 * @param {object} [options]
 * @param {boolean} [options.applyDefaults=true]
 *        When false, skip `defaultValue`, `"today"`, and `prefillFrom` so an
 *        edit screen shows only values that were actually stored.
 * @returns {object} Initial form values keyed by field.name.
 */
export const buildInitialData = (formConfig = [], rawAsset = {}, dropdownData = {}, tenantId, options = {}) => {
  const { applyDefaults = true } = options;
  const result = {};

  // Initialize fieldArrays first (minItems empty rows, or the raw array from the asset).
  (formConfig || []).forEach((item) => {
    if (item?.type !== "fieldArray") return;
    // Seed even when the array is currently hidden, so rows exist as soon as
    // the visibleWhen gate that hides it unlocks.
    const arrayName = getFieldArrayName(item);
    const minItems = Math.max(1, Number(item.minItems) || 1);
    const rawRows = rawAsset[arrayName];
    if (Array.isArray(rawRows) && rawRows.length > 0) {
      result[arrayName] = rawRows.map((row) => {
        const next = {
          ...createEmptyFieldArrayItem(item.children),
          ...(row && typeof row === "object" ? row : {}),
        };
        (item.children || []).forEach((child) => {
          const childName = child?.field?.name;
          if (!childName || child?.field?.type !== "radio") return;
          // API rows often store COMMON_GENDER_MALE; the radio wants MALE.
          next[childName] = coerceRadioFieldValue(next[childName]);
        });
        return next;
      });
    } else {
      result[arrayName] = Array.from({ length: minItems }, () =>
        createEmptyFieldArrayItem(item.children)
      );
    }
  });

  const fieldArrayChildNames = new Set(
    (formConfig || [])
      .filter((item) => item?.type === "fieldArray")
      .flatMap((item) => (item.children || []).map((c) => c?.field?.name).filter(Boolean))
  );

  flattenFormConfig(formConfig).forEach((item) => {
    const field = item.field;
    if (!field) return;
    const { name, type, dataSource } = field;

    // A fieldArray child's value lives on each row, not as a top-level form key.
    if (fieldArrayChildNames.has(name)) return;

    if (type === "dropdown") {
      if (dataSource?.defaultValueSource === "tenantId") {
        // Prefer the hook-built option (it carries the TRANSLATED city name);
        // synthesize from the raw tenantId only as a fallback.
        result[name] = dropdownData[name]?.[0] || toDropdownOption(tenantId, tenantId);
        return;
      }
      const rawVal = rawAsset[name] ?? (applyDefaults ? field.defaultValue : undefined);
      const rawNameHint = rawAsset[`${name}Name`];
      const staticOptions = item.options || [];
      const optionsList =
        dropdownData[name] || dropdownData[item.key] || staticOptions;
      const strict = field.strictOptions === true || dataSource?.strictOptions === true;
      result[name] = rawVal
        ? resolveOption(rawVal, rawNameHint, optionsList, { strict })
        : null;
      return;
    }

    if (type === "radio") {
      const apiAlias = item.apiFieldName;
      const rawVal =
        rawAsset[name] ??
        (apiAlias ? rawAsset[apiAlias] : undefined) ??
        "";
      result[name] = coerceRadioFieldValue(rawVal);
      return;
    }

    if (type === "file") {
      const raw = rawAsset[name] ?? (item.apiFieldName ? rawAsset[item.apiFieldName] : undefined);
      if (!raw) {
        result[name] = null;
        return;
      }
      if (typeof raw === "object") {
        result[name] = raw;
        return;
      }
      // APIs often return only the file-store id string; the file input expects an object.
      result[name] = { filestoreId: raw, documentuuid: raw, documentType: name };
      return;
    }

    const raw =
      rawAsset[name] ??
      (item.apiFieldName ? rawAsset[item.apiFieldName] : undefined);
    const isEmpty = raw === undefined || raw === null || raw === "";
    if (applyDefaults && isEmpty && field.prefillFrom) {
      const fromVal = readPrefillPath(rawAsset, field.prefillFrom);
      result[name] = fromVal !== undefined && fromVal !== null ? fromVal : "";
      return;
    }
    if (applyDefaults && isEmpty && field.defaultValue !== undefined && field.defaultValue !== null) {
      // "today" → local yyyy-MM-dd via toInputDate (avoids UTC day-shift).
      if (field.defaultValue === "today" && type === "date") {
        result[name] = toInputDate(new Date());
        return;
      }
      result[name] = field.defaultValue;
      return;
    }
    // Numbers become strings so controlled text inputs don't flip between types.
    result[name] =
      raw === undefined || raw === null
        ? ""
        : typeof raw === "number"
          ? String(raw)
          : raw;
  });

  return result;
};

/* ── submit payload ─────────────────────────────────────────────────── */

/**
 * Flatten formData for wizard session storage / step persistence:
 *   `{ assetType: { code: "LAND", name: "Land" } }`
 *     → `{ assetType: "LAND", assetTypeName: "Land" }`
 *
 * Non-dropdown (plain string/number) values pass through unchanged.
 * Preserves MDMS option metadata (multiplier, rentLabelKey) as flattened keys.
 *
 * For richer API mapping (apiFieldName renames, numeric casts,
 * staticFields/computedFields) use payloadUtils.buildApiPayload — this
 * intentionally stays a dumb flattener.
 *
 * @param {object} [formData={}] Live form values keyed by field name.
 * @returns {object} Flattened payload suitable for wizard session storage.
 */
export const buildPayload = (formData = {}) => {
  const payload = {};
  Object.entries(formData).forEach(([key, value]) => {
    if (value && typeof value === "object" && "code" in value) {
      payload[key] = value.code;
      payload[`${key}Name`] = value.name ?? value.i18nKey ?? value.code;
      // Keep MDMS option metadata for config-driven rent/label resolution after flatten.
      const multiplier =
        value.multiplier ?? value.rentMultiplier ?? value.cycleMultiplier;
      if (multiplier != null && multiplier !== "") {
        payload[`${key}Multiplier`] = multiplier;
      }
      if (value.rentLabelKey) payload[`${key}RentLabelKey`] = value.rentLabelKey;
    } else {
      payload[key] = value;
    }
  });
  return payload;
};

/* ── date helpers (shared by DynamicFormField / summary pages) ──────── */

/**
 * Parse a value into a Date object.
 * Accepts "yyyy-MM-dd" strings, epoch millis, or Date objects.
 *
 * @param {string|number|Date|null|undefined} v  Input value.
 * @returns {Date|null} Parsed Date, or null when input is invalid/empty.
 */
export const toDate = (v) => {
  if (!v && v !== 0) return null;
  const d = v instanceof Date ? v : new Date(v);
  return isNaN(d.getTime()) ? null : d;
};

/**
 * Format a value as a local yyyy-MM-dd string for native `<input type="date">`.
 * Built from local date parts — toISOString() shifts IST-midnight epochs back a day (UTC).
 * Already-formatted yyyy-MM-dd strings pass through unchanged.
 *
 * @param {string|number|Date|null|undefined} v  Input value.
 * @returns {string} yyyy-MM-dd string, or empty string when unparseable.
 */
export const toInputDate = (v) => {
  if (typeof v === "string" && /^\d{4}-\d{2}-\d{2}$/.test(v)) return v;
  const d = toDate(v);
  if (!d) return "";
  const pad = (n) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
};

/**
 * Resolve a workbench date bound for DatePicker min/max and validators.
 *
 * Supported shapes (all config-driven):
 * - `"today"` → local today (yyyy-MM-dd)
 * - `"yyyy-MM-dd"` → absolute date
 * - `"-3months"` / `"+1months"` / `"-30days"` / `"+1years"` → relative to today
 * - `{ "months": -3 }` / `{ "days": -30 }` / `{ "years": 1 }` → relative object
 *
 * @param {string|object|null|undefined} spec
 * @param {Date} [now=new Date()]
 * @returns {string} yyyy-MM-dd, or empty string when unset/invalid
 */
export const resolveConfigDate = (spec, now = new Date()) => {
  if (spec == null || spec === "") return "";
  if (typeof spec === "string") {
    const trimmed = spec.trim();
    if (trimmed.toLowerCase() === "today") return toInputDate(now);
    if (/^\d{4}-\d{2}-\d{2}$/.test(trimmed)) return trimmed;
    const relative = trimmed.match(/^([+-]?\d+)\s*(days?|months?|years?)$/i);
    if (relative) {
      const amount = Number(relative[1]);
      const unit = relative[2].toLowerCase();
      const d = new Date(now.getFullYear(), now.getMonth(), now.getDate());
      if (unit.startsWith("day")) d.setDate(d.getDate() + amount);
      else if (unit.startsWith("month")) d.setMonth(d.getMonth() + amount);
      else if (unit.startsWith("year")) d.setFullYear(d.getFullYear() + amount);
      return toInputDate(d);
    }
    return "";
  }
  if (typeof spec === "object") {
    const d = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    if (Number.isFinite(Number(spec.years))) d.setFullYear(d.getFullYear() + Number(spec.years));
    if (Number.isFinite(Number(spec.months))) d.setMonth(d.getMonth() + Number(spec.months));
    if (Number.isFinite(Number(spec.days))) d.setDate(d.getDate() + Number(spec.days));
    if (
      spec.years == null &&
      spec.months == null &&
      spec.days == null
    ) {
      return "";
    }
    return toInputDate(d);
  }
  return "";
};

/**
 * Current financial year as local calendar dates (1 April – 31 March).
 * January–March belong to the year that started the previous April.
 *
 * @param {Date} [now=new Date()]
 * @returns {{ start: string, end: string }} yyyy-MM-dd bounds
 */
export const financialYearBounds = (now = new Date()) => {
  const year = now.getFullYear();
  const startYear = now.getMonth() < 3 ? year - 1 : year;
  return {
    start: `${startYear}-04-01`,
    end: `${startYear + 1}-03-31`,
  };
};

/**
 * Date-picker bounds for one field.
 * `minDate` / `maxDate` still apply. When `field.withinFinancialYear` is set,
 * the window stays inside the current financial year and cannot pass today:
 * a minDate earlier than 1 April is raised to that day, and the latest day is
 * today. A maxDate after today, before 1 April, or missing is replaced by today.
 *
 * @param {object} [field={}]
 * @param {Date} [now=new Date()]
 * @returns {{ min: string, max: string }} yyyy-MM-dd, or "" when that side is unset
 */
export const resolveFieldDateBounds = (field = {}, now = new Date()) => {
  let min = resolveConfigDate(field.minDate, now);
  let max = resolveConfigDate(field.maxDate, now);
  if (!field.withinFinancialYear) return { min, max };

  const { start, end } = financialYearBounds(now);
  const today = toInputDate(now);
  if (!min || min < start) min = start;
  if (min > today) min = today;
  // A maxDate inside the year and on or before today can tighten the window.
  // Anything later than today, earlier than 1 April, or missing becomes today.
  if (!max || max > today || max < start) max = today;
  if (max > end) max = end;
  return { min, max };
};

/* ── DOM helper ─────────────────────────────────────────────────────── */

/**
 * Scroll the first element tagged `data-field-error="true"` into view.
 * DynamicFormField sets that attribute on errored fields.
 * Uses requestAnimationFrame so the DOM has updated before querying.
 *
 * @returns {void}
 */
export const scrollToFirstError = () => {
  requestAnimationFrame(() => {
    const behavior = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches
      ? "auto"
      : "smooth";
    document.querySelector('[data-field-error="true"]')?.scrollIntoView({
      behavior,
      block: "nearest",
      inline: "nearest",
    });
  });
};
