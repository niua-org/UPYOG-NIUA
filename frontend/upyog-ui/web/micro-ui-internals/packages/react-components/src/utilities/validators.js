/**
 * validators.js
 *
 * Field-level and cross-field validation plus computed-field helpers for
 * DynamicForm. Each field rule is a pure function: (value, ctx) => boolean
 * where ctx = { fieldConfig, formData }. Field rules run per visible leaf;
 * cross-field rules run once against the whole formData.
 *
 * Exports
 * -------
 * - fieldRules / registerFieldRule — extensible per-field validators
 * - validateFields                 — scan form config → { [fieldName]: true }
 * - validateCrossField             — routeConfig.crossFieldValidations
 * - calculateDuration / formatDurationDisplay
 * - calculateRentByBillingCycle / MAX_TAX_AMOUNT
 *
 * @see DynamicForm (COMPUTE_REGISTRY, goNext)
 * @see formUtils.isFieldVisible
 */

import { getFieldArrayName, resolveBillingCycleMultiplier, toDate, isFieldVisible, optionCode, toInputDate, resolveConfigDate, resolveFieldDateBounds, financialYearBounds, evaluateFormRule } from "./formUtils";

/**
 * True when a field value is considered empty for required checks.
 * Dropdowns need a selected option with .code.
 *
 * @param {*}      value
 * @param {string} type - field.type
 * @returns {boolean}
 */
const isEmptyValue = (value, type) => {
  if (type === "dropdown") return !value || !optionCode(value);
  if (type === "radio") {
    if (value && typeof value === "object") return !optionCode(value);
    return value === undefined || value === null || String(value).trim() === "";
  }
  return value === undefined || value === null || String(value).trim() === "";
};

/**
 * Compare yyyy-MM-dd (or parseable) values as local calendar days.
 * @returns {number|null} negative / 0 / positive, or null when either side is invalid
 */
const compareInputDates = (left, right) => {
  const a = toInputDate(left);
  const b = toInputDate(right);
  if (!a || !b) return null;
  if (a === b) return 0;
  return a < b ? -1 : 1;
};

/**
 * Built-in field validators keyed by validation block property names
 * (required, pattern, maxLength, maxAmount). Unknown keys are ignored.
 */
export const fieldRules = {
  /**
   * @param {*} value
   * @param {{ fieldConfig: object }} ctx
   * @returns {boolean}
   */
  required: (value, { fieldConfig }) =>
    !isEmptyValue(value, fieldConfig.field.type),

  /**
   * Regex test against validation.pattern. Empty values pass (use required).
   * Prefer patterns that avoid `\.` escaping (use `[.]`) so MDMS/JSON
   * double-escaping cannot turn a valid email into a false failure.
   *
   * @param {*} value
   * @param {{ fieldConfig: object }} ctx
   * @returns {boolean}
   */
  pattern: (value, { fieldConfig }) => {
    const { type } = fieldConfig.field;
    const { pattern } = fieldConfig.validation;
    if (!value || type === "dropdown") return true;
    const normalized = String(value).trim();
    if (!normalized) return true; // empty handled by `required`
    try {
      // `g` makes RegExp.test stateful, so a valid value can fail on the next keystroke.
      const flags = [
        ...new Set(String(fieldConfig.validation?.patternFlags || "").match(/[imsuy]/g) || []),
      ].join("");
      return new RegExp(pattern, flags).test(normalized);
    } catch (e) {
      console.error("Invalid validation.pattern:", pattern, e);
      return true;
    }
  },

  /**
   * @param {*} value
   * @param {{ fieldConfig: object }} ctx
   * @returns {boolean}
   */
  maxLength: (value, { fieldConfig }) => {
    const { maxLength } = fieldConfig.validation;
    if (!value) return true;
    return String(value).length <= maxLength;
  },

  /**
   * Billing amounts must fit numeric(12,2) — max 9999999999.99 by default.
   * Override via validation.maxAmount.
   *
   * @param {*} value
   * @param {{ fieldConfig: object }} ctx
   * @returns {boolean}
   */
  maxAmount: (value, { fieldConfig }) => {
    const max = fieldConfig.validation.maxAmount ?? 9999999999.99;
    if (value === null || value === undefined || value === "") return true;
    const num = Number(String(value).replace(/,/g, "").trim());
    return Number.isFinite(num) && num >= 0 && num <= max;
  },

  /**
   * Date must be on/after the effective minimum.
   * Empty values pass (use required). A withinFinancialYear field uses the
   * later of minDate and 1 April, so a looser minDate cannot leave the year.
   */
  minDate: (value, { fieldConfig }) => {
    if (!value) return true;
    const spec =
      fieldConfig.validation?.minDate === true ||
      fieldConfig.validation?.minDate == null
        ? fieldConfig.field?.minDate
        : fieldConfig.validation.minDate;
    const bound = fieldConfig.field?.withinFinancialYear
      ? resolveFieldDateBounds({ ...fieldConfig.field, minDate: spec }).min
      : resolveConfigDate(spec);
    if (!bound) return true;
    const cmp = compareInputDates(value, bound);
    return cmp == null ? true : cmp >= 0;
  },

  /**
   * Date must be on/before field.maxDate (or validation.maxDate override).
   * Empty values pass (use required). Spec uses resolveConfigDate shapes.
   */
  maxDate: (value, { fieldConfig }) => {
    if (!value) return true;
    const spec =
      fieldConfig.validation?.maxDate === true ||
      fieldConfig.validation?.maxDate == null
        ? fieldConfig.field?.maxDate
        : fieldConfig.validation.maxDate;
    const bound = fieldConfig.field?.withinFinancialYear
      ? resolveFieldDateBounds({ ...fieldConfig.field, maxDate: spec }).max
      : resolveConfigDate(spec);
    if (!bound) return true;
    const cmp = compareInputDates(value, bound);
    return cmp == null ? true : cmp <= 0;
  },

  /**
   * Date must fall in the current financial year (1 April – 31 March).
   * Empty values pass (use required).
   * A `withinFinancialYear` field also cannot be after today, matching the
   * date-picker max from resolveFieldDateBounds. A typed future date in the
   * same year would otherwise pass while the picker refuses it.
   */
  financialYear: (value, { fieldConfig } = {}) => {
    if (!value) return true;
    const { start, end } = financialYearBounds();
    // Picker max is today. Cap the check the same way so a typed future date fails.
    let latest = end;
    if (fieldConfig?.field?.withinFinancialYear) {
      const today = toInputDate(new Date());
      if (today && today < latest) latest = today;
    }
    const cmpStart = compareInputDates(value, start);
    const cmpEnd = compareInputDates(value, latest);
    if (cmpStart == null || cmpEnd == null) return true;
    return cmpStart >= 0 && cmpEnd <= 0;
  },

  /**
   * Numeric value must be strictly greater than validation.gt (ATT: UOM > 0).
   */
  gt: (value, { fieldConfig }) => {
    if (value === null || value === undefined || value === "") return true;
    const threshold = Number(fieldConfig.validation?.gt);
    if (!Number.isFinite(threshold)) return true;
    const num = Number(String(value).replace(/,/g, "").trim());
    return Number.isFinite(num) && num > threshold;
  },

  /**
   * Numeric min — absolute number or `{ fromField: "otherField" }` on the same row/form.
   */
  min: (value, { fieldConfig, formData }) => {
    if (value === null || value === undefined || value === "") return true;
    const spec = fieldConfig.validation?.min;
    let bound = null;
    if (spec && typeof spec === "object" && spec.fromField) {
      const raw = formData?.[spec.fromField];
      if (raw === null || raw === undefined || raw === "") return true;
      bound = Number(raw);
    } else {
      bound = Number(spec);
    }
    if (!Number.isFinite(bound)) return true;
    const num = Number(String(value).replace(/,/g, "").trim());
    return Number.isFinite(num) && num >= bound;
  },

  /**
   * Numeric max — absolute number or `{ fromField: "otherField" }` on the same row/form.
   */
  max: (value, { fieldConfig, formData }) => {
    if (value === null || value === undefined || value === "") return true;
    const spec = fieldConfig.validation?.max;
    let bound = null;
    if (spec && typeof spec === "object" && spec.fromField) {
      const raw = formData?.[spec.fromField];
      if (raw === null || raw === undefined || raw === "") return true;
      bound = Number(raw);
    } else {
      bound = Number(spec);
    }
    if (!Number.isFinite(bound)) return true;
    const num = Number(String(value).replace(/,/g, "").trim());
    return Number.isFinite(num) && num <= bound;
  },

  /**
   * Required when another field matches a rule (e.g. UOM required when unit is set).
   */
  requiredWhen: (value, { fieldConfig, formData }) => {
    const rule = fieldConfig.validation?.requiredWhen;
    if (!rule) return true;
    if (!evaluateFormRule(rule, formData || {})) return true;
    return !isEmptyValue(value, fieldConfig.field?.type);
  },
};

/**
 * Register a custom field rule at runtime, e.g. registerFieldRule('min', fn).
 *
 * @param {string}   name - Key matching validation.<name> in field config.
 * @param {Function} fn   - (value, ctx) => boolean.
 */
export function registerFieldRule(name, fn) {
  fieldRules[name] = fn;
}

/**
 * Validates every visible leaf field against its `validation` block.
 * fieldArray children are validated per row; error keys are
 * `${arrayName}.${index}.${childName}`.
 *
 * @param {array}  formConfig - routeConfig.form
 * @param {object} formData   - Current DynamicForm state
 * @returns {object} Map of fieldName → true when invalid
 */
export function validateFields(formConfig, formData) {
  const errors = {};

  const validateLeaf = (fieldConfig, data, errorKey) => {
    const { field } = fieldConfig;
    if (!field) return;
    if (!isFieldVisible(fieldConfig, data)) return;

    const validation = { ...(fieldConfig.validation || {}) };
    // Config-driven date bounds on field.minDate / field.maxDate also validate.
    if (field.minDate != null && validation.minDate === undefined) {
      validation.minDate = true;
    }
    if (field.maxDate != null && validation.maxDate === undefined) {
      validation.maxDate = true;
    }
    // Financial year is a hard limit. A looser minDate cannot turn it off.
    if (field.withinFinancialYear) {
      validation.financialYear = true;
    }

    const value = data[field.name];
    const ctx = { fieldConfig: { ...fieldConfig, validation }, formData: data };
    const ruleNames = Object.keys(validation);
    const orderedRuleNames = validation.financialYear
      ? ["financialYear", ...ruleNames.filter((name) => name !== "financialYear")]
      : ruleNames;

    for (const ruleName of orderedRuleNames) {
      const rule = fieldRules[ruleName];
      if (!rule) continue;
      if (validation[ruleName] === false) continue;
      if (!rule(value, ctx)) {
        errors[errorKey] = ruleName;
        break;
      }
    }
  };

  const fieldArrayChildNames = new Set(
    (formConfig || [])
      .filter((item) => item?.type === "fieldArray")
      .flatMap((item) => (item.children || []).map((c) => c?.field?.name).filter(Boolean))
  );

  (formConfig || []).forEach((item) => {
    if (item?.type === "fieldArray") {
      if (!isFieldVisible(item, formData)) return;
      const name = getFieldArrayName(item);
      const rows = Array.isArray(formData[name]) ? formData[name] : [];
      const minItems = Math.max(1, Number(item.minItems) || 1);
      const effectiveRows =
        rows.length > 0
          ? rows
          : Array.from({ length: minItems }, () => ({}));

      effectiveRows.forEach((row, index) => {
        (item.children || []).forEach((child) => {
          if (!child?.field?.name) return;
          validateLeaf(child, row || {}, `${name}.${index}.${child.field.name}`);
        });
      });
      return;
    }

    if (item?.type === "group") {
      if (!isFieldVisible(item, formData)) return;
      (item.children || []).forEach((child) => {
        if (!child?.field?.name) return;
        if (fieldArrayChildNames.has(child.field.name)) return;
        validateLeaf(child, formData, child.field.name);
      });
      return;
    }

    if (item?.type === "sectionHeader") return;
    if (!item?.field?.name) return;
    if (fieldArrayChildNames.has(item.field.name)) return;
    validateLeaf(item, formData, item.field.name);
  });

  return errors;
}

/**
 * Cross-field validators declared in routeConfig.crossFieldValidations:
 * [{ id, fields: [...names], validate: (formData) => boolean, message }]
 * Kept config-driven so dimension/area-style checks aren't hardcoded in the form.
 *
 * @param {array}  [crossFieldValidations]
 * @param {object} formData
 * @returns {{ errors: object, failures: array }}
 */
export function validateCrossField(crossFieldValidations = [], formData) {
  const errors = {};
  const failures = [];

  crossFieldValidations.forEach((rule) => {
    if (!rule.validate(formData)) {
      rule.fields.forEach((f) => { errors[f] = true; });
      failures.push(rule);
    }
  });

  return { errors, failures };
}

// ── computed-field helpers (COMPUTE_REGISTRY in DynamicForm) ────────────

/**
 * Whole months between start and end dates as a string (API stores int months),
 * or "" until both dates are valid / end >= start.
 * Accepts "yyyy-MM-dd" strings, epoch millis, or Date objects.
 * Use formatDurationDisplay() for the auto-populated UI text.
 *
 * @param {string|number|Date} startDate
 * @param {string|number|Date} endDate
 * @returns {string}
 */
export const calculateDuration = (startDate, endDate) => {
  const start = toDate(startDate);
  const end = toDate(endDate);
  if (!start || !end || end < start) return "";

  let months =
    (end.getFullYear() - start.getFullYear()) * 12 +
    (end.getMonth() - start.getMonth());
  if (end.getDate() < start.getDate()) months -= 1; // don't count a partial month

  return months >= 0 ? String(months) : "";
};

/**
 * UI label for duration months: "N months" or "Y years M months" when > 12.
 *
 * @param {string|number|null|undefined} totalMonths
 * @returns {string}
 */
export const formatDurationDisplay = (totalMonths) => {
  if (totalMonths === null || totalMonths === undefined || totalMonths === "") return "";
  const months = Number(totalMonths);
  if (!Number.isFinite(months) || months < 0) return "";
  if (months <= 12) return  `${months} ${months === 1 ? "month" : "months"}`;

  const years = Math.floor(months / 12);
  const rem = months % 12;
  const parts = [];
  if (years) parts.push(`${years} ${years === 1 ? "year" : "years"}`);
  if (rem) parts.push(`${rem} ${rem === 1 ? "month" : "months"}`);
  return parts.join(" ");
};

/** Matches billing DB column numeric(12,2) — values must be below 10^10. */
export const MAX_TAX_AMOUNT = 9999999999.99;

/**
 * Parse a positive number from a form string (strips commas). Returns 0 if invalid.
 * @param {*} value
 * @returns {number}
 */
const toPositiveNumber = (value) => {
  const num = Number(String(value ?? "").replace(/,/g, "").trim());
  return Number.isFinite(num) && num > 0 ? num : 0;
};

/**
 * Format a computed rent amount for form storage; "" if out of range.
 * @param {number} value
 * @returns {string}
 */
const formatRentAmount = (value) => {
  if (!Number.isFinite(value) || value <= 0 || value > MAX_TAX_AMOUNT) return "";
  const rounded = Math.round(value * 100) / 100;
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(2);
};

/**
 * Rent for the selected billing period = rate/sqft × plot area × cycle multiplier.
 * Used via field.computeFrom + field.computeFn in route config.
 * DynamicForm passes billingCycleOptions as a trailing arg.
 * Returns "0" when billing cycle is not selected.
 *
 * @param {*}      rentRate
 * @param {*}      totalFloorArea
 * @param {*}      billingCycle         Code or option object.
 * @param {array}  [billingCycleOptions] MDMS options with multipliers.
 * @returns {string}
 */
export const calculateRentByBillingCycle = (
  rentRate,
  totalFloorArea,
  billingCycle,
  billingCycleOptions = []
) => {
  // No billing cycle → rent (and advance payment via copyValue) stay at zero.
  if (!optionCode(billingCycle)) return "0";

  const rate = toPositiveNumber(rentRate);
  const area = toPositiveNumber(totalFloorArea);
  if (!rate || !area) return "0";

  const multiplier = resolveBillingCycleMultiplier(billingCycle, billingCycleOptions);
  return formatRentAmount(rate * area * multiplier) || "0";
};
