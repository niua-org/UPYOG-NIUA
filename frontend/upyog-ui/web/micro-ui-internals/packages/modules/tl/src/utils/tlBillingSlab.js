/**
 * tlBillingSlab.js
 *
 * TL-only billing-slab behaviour for DynamicForm fieldArrays.
 * Workbench JSON stays module-agnostic; this module owns slab lookup,
 * UOM prefill, range meta (fromUom/toUom), and the billingSlabFound rule.
 *
 * Wire via TLCitizenNewApplication:
 *   enrichFieldArrayRow + registerFieldRule("billingSlabFound", ...)
 *   + tlFormConfig field overlays (validation / messages).
 */

import { optionCode, registerFieldRule } from "@nudmcdgnpm/digit-ui-react-components";

const SLAB_META_FIELDS = ["fromUom", "toUom", "_billingSlabFound"];

const norm = (value) => String(value ?? "").trim().toUpperCase();

/** Compare ACC-1 with ACC_1 / "acc 1" without changing stored codes. */
const normCode = (value) => norm(value).replace(/[\s_-]+/g, "");

const structureCodesFromForm = (formData = {}) =>
  [optionCode(formData?.buildingType), optionCode(formData?.vehicleType)]
    .filter(Boolean)
    .map((c) => String(c));

const hasTradeType = (slab) => {
  const tradeType = norm(slab?.tradeType);
  return Boolean(tradeType) && tradeType !== "NULL";
};

/**
 * Accessory-only billing slab. Calculator looks these up by accessoryCategory
 * and applicationType, not by licenseType. tradeType is empty on these rows.
 */
const isAccessorySlab = (slab) =>
  Boolean(norm(slab?.accessoryCategory)) && !hasTradeType(slab);

/** Accessory slab usable in citizen create (niuatt SelectAccessoriesDetails). */
const isUsableAccessorySlab = (slab) =>
  isAccessorySlab(slab) &&
  Boolean(norm(slab?.uom)) &&
  slab?.rate != null &&
  Number(slab.rate) > 0;

const findTradeUnitSlab = (billingSlabs, tradeCode, formData) => {
  if (!tradeCode || !Array.isArray(billingSlabs)) return null;
  const structureCodes = structureCodesFromForm(formData);
  return (
    billingSlabs.find(
      (s) =>
        s?.tradeType &&
        String(s.tradeType).toUpperCase() === String(tradeCode).toUpperCase() &&
        structureCodes.includes(String(s?.structureType || ""))
    ) || null
  );
};

const findAccessorySlab = (billingSlabs, accessoryCode) => {
  if (!accessoryCode || !Array.isArray(billingSlabs)) return null;
  const code = normCode(accessoryCode);
  const matches = billingSlabs.filter(
    (s) => s?.accessoryCategory && normCode(s.accessoryCategory) === code
  );
  if (!matches.length) return null;
  return (
    matches.find(isUsableAccessorySlab) ||
    matches.find((s) => norm(s?.uom)) ||
    matches[0]
  );
};

const clearSlabMeta = (row) => {
  SLAB_META_FIELDS.forEach((f) => {
    row[f] = f === "_billingSlabFound" ? undefined : null;
  });
};

/**
 * Apply billing-slab side effects when a fieldArray child changes.
 * Used as DynamicForm `enrichFieldArrayRow`.
 *
 * @param {object} ctx
 * @param {string} ctx.childName
 * @param {*} ctx.value
 * @param {object} ctx.row
 * @param {object} ctx.formData
 * @param {string[]} [ctx.resetFields]
 * @param {object[]} [ctx.billingSlabs]
 * @returns {object} next row
 */
export const enrichTlBillingSlabRow = ({
  childName,
  value,
  row = {},
  formData = {},
  resetFields = [],
  billingSlabs = [],
}) => {
  const updated = { ...row, [childName]: value };
  (resetFields || []).forEach((f) => {
    updated[f] = null;
  });

  // Cascade parents clear slab meta even when JSON resetFields omit them.
  if (["tradecategory", "tradetype", "tradesubtype", "accessory"].includes(childName)) {
    clearSlabMeta(updated);
  }

  if (childName === "tradesubtype") {
    const tradeCode = optionCode(value);
    const slab = findTradeUnitSlab(billingSlabs, tradeCode, formData);
    if (slab) {
      updated.unit = slab.uom || null;
      updated.fromUom = slab.fromUom;
      updated.toUom = slab.toUom;
      updated._billingSlabFound = true;
    } else {
      updated.unit = null;
      updated.fromUom = null;
      updated.toUom = null;
      updated._billingSlabFound = tradeCode ? false : undefined;
    }
    if (!(resetFields || []).includes("uom")) updated.uom = "";
  }

  if (childName === "accessory") {
    const accessoryCode = optionCode(value);
    const slab = findAccessorySlab(billingSlabs, accessoryCode);
    if (slab) {
      updated.unit = slab.uom || null;
      updated.fromUom = slab.fromUom ?? null;
      updated.toUom = slab.toUom ?? null;
      updated._billingSlabFound = true;
    } else {
      // Fall back to MDMS AccessoriesCategory.uom when slab has no row yet.
      // Only flag a miss when accessory slabs were actually loaded — trade-only
      // slabs must not make every accessory look unpriced.
      const mdmsUom =
        value && typeof value === "object" ? value.uom : null;
      updated.unit = mdmsUom || null;
      updated.fromUom = null;
      updated.toUom = null;
      const accessorySlabsLoaded = billingSlabs.some(isAccessorySlab);
      updated._billingSlabFound =
        accessoryCode && accessorySlabsLoaded ? false : undefined;
    }
    if (!(resetFields || []).includes("uom")) updated.uom = "";
    if (!(resetFields || []).includes("accessorycount")) {
      updated.accessorycount = "";
    }
  }

  return updated;
};

/**
 * Validator: subtype/accessory must resolve to a billing slab when selected.
 * @param {*} value
 * @param {{ formData?: object }} ctx
 * @returns {boolean}
 */
export const billingSlabFoundRule = (value, { formData }) => {
  if (!value) return true;
  return formData?._billingSlabFound !== false;
};

let billingSlabRuleRegistered = false;

/** Idempotent register of TL `billingSlabFound` field rule. */
export const ensureTlBillingSlabFieldRule = () => {
  if (billingSlabRuleRegistered) return;
  registerFieldRule("billingSlabFound", billingSlabFoundRule);
  billingSlabRuleRegistered = true;
};

/**
 * Filter API billing slabs for citizen NEW / PERMANENT create.
 * @param {object} data
 * @returns {object[]}
 */
export const selectCitizenNewBillingSlabs = (data) =>
  (data?.billingSlab || []).filter((slab) => {
    const applicationType = norm(slab?.applicationType);
    if (applicationType && applicationType !== "NEW") return false;
    // Accessory rows are not license-type scoped (calculator searches by
    // accessoryCategory + applicationType). Dropping them when licenseType
    // is null made every accessory selection look like a missing slab.
    if (isAccessorySlab(slab)) return true;
    return norm(slab?.licenseType) === "PERMANENT" && hasTradeType(slab);
  });

/** Trade-unit MDMS fields (category / type / subtype). */
const TRADE_UNIT_FIELDS = ["tradecategory", "tradetype", "tradesubtype"];

/** Subtype list is also restricted by billing slabs for the selected structure. */
const TRADE_SUBTYPE_SLAB_FIELDS = ["tradesubtype"];

/** Accessory dropdown — restricted to billing-slab accessory categories. */
const ACCESSORY_FIELDS = ["accessory"];

const isActiveTradeOption = (o) => o?.active === true;

/**
 * Trade-unit + accessory dropdown shaping for DynamicForm `transformDropdownData`:
 * 1. Keep only MDMS rows with `active === true` (category / type / subtype).
 * 2. For subtype, also intersect with NEW/PERMANENT billing slabs for the
 *    selected building/vehicle structure (niuatt usable list).
 * 3. For accessory, intersect with NEW/PERMANENT accessory slabs that have
 *    uom + rate > 0 (niuatt SelectAccessoriesDetails).
 *
 * @param {object} dropdownData
 * @param {object} formData
 * @param {object[]} billingSlabs
 * @returns {object}
 */
export const filterTradeUnitDropdownByBillingSlabs = (
  dropdownData = {},
  formData = {},
  billingSlabs = []
) => {
  let changed = false;
  const next = { ...dropdownData };

  TRADE_UNIT_FIELDS.forEach((name) => {
    const list = next[name];
    if (!Array.isArray(list) || !list.length) return;
    const filtered = list.filter(isActiveTradeOption);
    if (filtered.length !== list.length) {
      next[name] = filtered;
      changed = true;
    }
  });

  // AccessoriesCategory MDMS often uses active: "True" / true — keep truthy active.
  // Also normalize ATT i18n keys (ACC-1 → TRADELICENSE_ACCESSORIESCATEGORY_ACC_1).
  ACCESSORY_FIELDS.forEach((name) => {
    const list = next[name];
    if (!Array.isArray(list) || !list.length) return;
    const filtered = list
      .filter(
        (o) => o?.active !== false && o?.active !== "false" && o?.active !== "False"
      )
      .map((o) => {
        const code = o?.code;
        if (!code) return o;
        const i18nKey = `TRADELICENSE_ACCESSORIESCATEGORY_${String(code)
          .toUpperCase()
          .replace(/-/g, "_")}`;
        return o.i18nKey === i18nKey ? o : { ...o, i18nKey };
      });
    if (
      filtered.length !== list.length ||
      filtered.some((o, i) => o !== list[i])
    ) {
      next[name] = filtered;
      changed = true;
    }
  });

  const structureCodes = structureCodesFromForm(formData);
  // Structure chosen but slabs not loaded yet — do not expose unslabbed MDMS
  // subtypes (backend returns 400 INVALID TRADETYPE / INVALID UOM).
  // Accessories are not structure-scoped; leave them until slabs arrive.
  if (structureCodes.length && (!Array.isArray(billingSlabs) || !billingSlabs.length)) {
    TRADE_SUBTYPE_SLAB_FIELDS.forEach((name) => {
      if (Array.isArray(next[name]) && next[name].length) {
        next[name] = [];
        changed = true;
      }
    });
    return changed ? next : dropdownData;
  }

  if (Array.isArray(billingSlabs) && billingSlabs.length) {
    if (structureCodes.length) {
      const allowedTrade = new Set(
        billingSlabs
          .filter(
            (s) =>
              s?.tradeType && structureCodes.includes(String(s?.structureType || ""))
          )
          .map((s) => String(s.tradeType).toUpperCase())
      );
      TRADE_SUBTYPE_SLAB_FIELDS.forEach((name) => {
        const list = next[name];
        if (!Array.isArray(list) || !list.length) return;
        const filtered = allowedTrade.size
          ? list.filter((o) => allowedTrade.has(String(o?.code || "").toUpperCase()))
          : [];
        if (filtered.length !== list.length) {
          next[name] = filtered;
          changed = true;
        }
      });
    }

    // Accessory list from accessory-only slabs. Keep MDMS labels/uom, but
    // do not hide the list when the loaded slabs are trade units only.
    const allowedAcc = new Set(
      billingSlabs
        .filter(isAccessorySlab)
        .map((s) => normCode(s.accessoryCategory))
    );
    ACCESSORY_FIELDS.forEach((name) => {
      const list = next[name];
      if (!Array.isArray(list) || !list.length) return;
      if (!allowedAcc.size) return;
      const filtered = list.filter((o) => allowedAcc.has(normCode(o?.code)));
      if (filtered.length !== list.length) {
        next[name] = filtered;
        changed = true;
      }
    });
  }

  return changed ? next : dropdownData;
};
