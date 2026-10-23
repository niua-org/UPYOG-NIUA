/**
 * DynamicForm.js
 *
 * Config-driven form engine for UPYOG MDMS wizard steps and search pages.
 * Typically wrapped by DynamicFormStep for wizard routes; can also be used
 * directly with mode="search" for filter UIs that reuse the same MDMS field
 * definitions.
 *
 * Responsibilities
 * ----------------
 * 1. Load dropdown / MDMS options via useDynamicMDMS (localities follow the
 *    selected city tenant; other masters use stateId / tenantId).
 * 2. Hydrate form state from editData + persisted wizard data via
 *    buildInitialData; sync once after dropdowns finish loading (preserves
 *    non-empty user input).
 * 3. Render fields from routeConfig.form (sorted by order) through
 *    DynamicFormField — visibility, uploads, and field-level search panels
 *    included.
 * 4. Re-derive computed fields from COMPUTE_REGISTRY whenever a dependency
 *    in field.computeFrom changes (e.g. calculateDuration, copyValue).
 * 5. Validate per-field and cross-field rules on submit; scroll to first error.
 * 6. Build payloads with buildPayload:
 *    - wizard create → onSelect(config.key, { [payloadKey]: [formVal] })
 *    - wizard edit   → updateMutation.mutate(...) then onSubmit
 *    - search mode   → mapFormToSearchFilters → onSubmit({ payload, isSearch })
 * 7. Optional draft: explicit Save Draft button (onSaveDraft) and/or debounced
 *    auto-persist (onPersistDraft when showDraftButton is false).
 * 8. Optional field search (onFieldSearch) for estate / asset lookup panels,
 *    with optional typeahead when field.searchTypeahead === true.
 *
 * Computed fields (routeConfig.form)
 * ----------------------------------
 * Declare on a field:
 *   field: { computeFrom: ["startDate","endDate"], computeFn: "calculateDuration" }
 * When any computeFrom dependency changes, DynamicForm looks up computeFn in
 * COMPUTE_REGISTRY and updates the field. Add new pure functions to the
 * registry — no DynamicForm control-flow changes required.
 *
 * Modes
 * -----
 * - "wizard" (default): ActionBar with Save & Next (left) / Draft / Cancel (right).
 * - "search": SearchForm layout (inline row or stacked); no wizard ActionBar;
 *   submit maps values to search filters.
 *
 * Typical usage
 * -------------
 *   // Wizard (usually via DynamicFormStep)
 *   <DynamicForm
 *     routeConfig={mergedRouteConfig}
 *     config={{ key: stepKey }}
 *     onSelect={onSelect}
 *     onSubmit={handleSubmit}
 *     persistedData={sessionData}
 *     isEditMode={isEditMode}
 *     editData={editData}
 *     tenantId={tenantId}
 *     t={t}
 *     showDraftButton
 *     onSaveDraft={saveDraft}
 *     onFieldSearch={handleFieldSearch}
 *   />
 *
 *   // Search filters
 *   <DynamicForm
 *     mode="search"
 *     searchLayout="inline"
 *     routeConfig={searchRouteConfig}
 *     onSubmit={runSearch}
 *     tenantId={tenantId}
 *     t={t}
 *   />
 *
 * Props
 * -----
 * @param {object}   routeConfig           Merged MDMS route/step config (form, payloadKey,
 *                                         crossFieldValidations, actionButton, uploadModule,
 *                                         searchLayout, editPayloadExtras, etc.).
 * @param {Function} [onSubmit]            Called after successful goNext with
 *                                         { payload, response?, error?, isEditMode?, isSearch?, formValues? }.
 * @param {Function} [onSelect]            Wizard step select(key, data, skipStep) for create flow.
 * @param {object}   [config]              Step identity; config.key is passed to onSelect / draft.
 * @param {boolean}  [isEditMode=false]    Uses updateMutation instead of onSelect.
 * @param {boolean}  [isDisabled=false]    Disables field interaction and action buttons.
 * @param {object}   [updateMutation]      Required in edit mode; mutate(payload, { onSuccess, onError }).
 * @param {object}   [editData]            Existing record values for edit / prefill.
 * @param {string}   [tenantId]            Tenant for MDMS, uploads, and payloads.
 * @param {object}   [persistedData]       Wizard session data keyed by config.key / payloadKey.
 * @param {Function} [t]                   i18n translator; defaults to identity.
 * @param {boolean}  [showCancel=true]     Show Cancel / Clear in wizard ActionBar.
 * @param {string}   [cancelLabel]         i18n key for cancel (default CS_COMMON_CANCEL).
 * @param {boolean}  [confirmCancel=false] Show confirmation Modal before wizard Cancel reset (opt-in).
 * @param {string}   [cancelConfirmHeading] Modal heading i18n key (default CS_COMMON_CANCEL).
 * @param {string}   [cancelConfirmMessage] Modal body i18n key (default CS_WANT_TO_CANCEL).
 * @param {string}   [cancelConfirmYesLabel] Confirm button i18n key (default CS_COMMON_YES).
 * @param {string}   [cancelConfirmNoLabel] Dismiss button i18n key (default CS_COMMON_NO).
 * @param {Function} [onCancel]            Extra callback after form reset on cancel.
 * @param {object}   [resetBaseline]       Preferred reset source on cancel; use `{}` to clear all.
 *                                         Cancel skips field defaultValue/prefillFrom.
 * @param {boolean}  [showDraftButton]     Show explicit Save Draft button.
 * @param {string}   [draftLabel]          Draft button i18n key.
 * @param {string}   [draftSuccessLabel]   Toast after draft save.
 * @param {Function} [onSaveDraft]         (flatPayload) => void for explicit draft button.
 * @param {Function} [onPersistDraft]      Debounced auto-save when no draft button is shown.
 * @param {Function} [onFieldSearch]       async (fieldName, formSnapshot) =>
 *                                         { prefill } | { matches } | { found } | { notFound } | { error }.
 * @param {Function} [onPrefillApplied]    (prefill) => void after a search result is applied.
 * @param {string}   [mode="wizard"]       "wizard" | "search".
 * @param {string}   [searchLayout]        "inline" | "stack"; overrides routeConfig.searchLayout.
 * @param {string}   [layout]              "accordion" groups fields by `sections` (JSON body[]).
 * @param {Array}    [sections]            Accordion panels from buildAccordionSections.
 *
 * @see DynamicFormStep
 * @see DynamicFormField
 * @see useDynamicMDMS
 * @see buildInitialData
 * @see buildPayload
 */

import React, { useState, useCallback, useMemo, useEffect, useRef } from "react";
import { useLocation } from "react-router-dom";
import { resolveConfigTimelineStep } from "./ConfigTimeline";
import { SubmitBar, Toast, Loader } from "@nudmcdgnpm/digit-ui-react-components";
import ActionBar from "../atoms/ActionBar";
import ButtonSelector from "../atoms/ButtonSelector";
import CardText from "../atoms/CardText";
import Modal from "../hoc/Modal";
import DynamicFormField from "./DynamicFormField";
import DynamicFormAccordion from "./DynamicFormAccordion";
import { validateFields, validateCrossField, calculateDuration, calculateRentByBillingCycle } from "../utilities/validators";
import { sortByOrder, buildPayload, scrollToFirstError, buildInitialData, flattenFormConfig, findFieldConfig, enrichDropdownSelection, optionCode, collectSectionFieldNames, findAccordionSectionId, getAccordionCellClass, isFieldVisible, getFieldArrayName, createEmptyFieldArrayItem, errorKeyBelongsTo } from "../utilities/formUtils";
import {
  resolveFormGate,
  resolveCreateNewPath,
  shouldShowGatedActionBar,
  resolveAppPath,
} from "../utilities/formGateUtils";
import useDynamicMDMS from "../utilities/useDynamicMDMS";
import { mapFormToSearchFilters } from "../utilities/searchUtils";
import { SearchField, SearchForm } from "./SearchForm";

const CancelPopupHeading = ({ label }) => <h1 className="heading-m">{label}</h1>;

const CancelPopupCloseIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="#FFFFFF">
    <path d="M0 0h24v24H0V0z" fill="none" />
    <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12 19 6.41z" />
  </svg>
);

const CancelPopupCloseBtn = ({ onClick }) => (
  <div className="icon-bg-secondary" onClick={onClick}>
    <CancelPopupCloseIcon />
  </div>
);

/**
 * Adapts SearchForm's react-hook-form-style handleSubmit API.
 * Returns an event handler that prevents default and invokes `onValid`
 * (typically goNext) so SearchForm can submit without RHF installed.
 *
 * @param {Function} onValid - Callback to run on successful form submit.
 * @returns {(e?: Event) => void}
 */
const searchFormHandleSubmit = (onValid) => (e) => {
  if (e?.preventDefault) e.preventDefault();
  onValid();
};

/**
 * Named compute functions referenced by field.computeFn in MDMS / routeConfig.
 * Declare on a field: { computeFrom: ["startDate","endDate"], computeFn: "calculateDuration" }.
 * Add new pure functions here as modules need derived fields — no DynamicForm
 * control-flow changes required.
 */
const COMPUTE_REGISTRY = {
  calculateDuration,
  calculateRentByBillingCycle,
  /**
   * Copies a single upstream field value (e.g. advancePayment ← monthlyRent).
   * Empty/null/undefined inputs become "".
   *
   * @param {*} value - Upstream field value.
   * @returns {*|string}
   */
  copyValue: (value) =>
    value === undefined || value === null || value === "" ? "" : value,
};

/**
 * True when an existing error key belongs to a field the user just edited.
 * Prefixes cover field-array keys such as `tradeUnits.0.uom`.
 *
 * @param {string}   key
 * @param {Set<string>} keys
 * @param {string[]} prefixes
 * @returns {boolean}
 */
const isTouchedErrorKey = (key, keys, prefixes) =>
  keys.has(key) || prefixes.some((prefix) => key === prefix || key.startsWith(`${prefix}.`));

/**
 * Field-array names whose row count changed.
 * Index-based error keys (`owners.1.name`) no longer point at the same row,
 * so those messages are dropped instead of being left on the next row.
 */
const resizedArrayNames = (before = {}, after = {}) => {
  const names = [];
  const keys = new Set([...Object.keys(before || {}), ...Object.keys(after || {})]);
  keys.forEach((name) => {
    const nextRows = after?.[name];
    const prevRows = before?.[name];
    if (!Array.isArray(nextRows) && !Array.isArray(prevRows)) return;
    const nextLen = Array.isArray(nextRows) ? nextRows.length : 0;
    const prevLen = Array.isArray(prevRows) ? prevRows.length : 0;
    if (nextLen !== prevLen) names.push(name);
  });
  return names;
};

/** Remove error keys that belong to the given field or field-array prefixes. */
const dropErrorPrefixes = (errors, prefixes = []) => {
  if (!prefixes.length) return errors;
  const next = { ...errors };
  Object.keys(next).forEach((key) => {
    if (prefixes.some((prefix) => key === prefix || key.startsWith(`${prefix}.`))) {
      delete next[key];
    }
  });
  return next;
};

/**
 * Error keys for field-array cells whose values changed in this update.
 * Removed rows are included so a deleted `owners.2.name` message does not
 * stay on screen after the row is gone. Same-reference arrays are skipped.
 */
const changedArrayCellKeys = (before = {}, after = {}) => {
  const keys = [];
  const names = new Set([
    ...Object.keys(before || {}),
    ...Object.keys(after || {}),
  ]);
  names.forEach((name) => {
    const nextRows = after?.[name];
    const prevRows = before?.[name];
    const nextIsArray = Array.isArray(nextRows);
    const prevIsArray = Array.isArray(prevRows);
    if (!nextIsArray && !prevIsArray) return;
    if (nextRows === prevRows) return;

    const nextList = nextIsArray ? nextRows : [];
    const prevList = prevIsArray ? prevRows : [];
    const length = Math.max(nextList.length, prevList.length);
    for (let index = 0; index < length; index += 1) {
      const row = nextList[index];
      const prev = prevList[index];
      const prevCells = prev && typeof prev === "object" ? prev : {};
      if (row && typeof row === "object") {
        Object.keys(row).forEach((child) => {
          if (row[child] !== prevCells[child]) keys.push(`${name}.${index}.${child}`);
        });
        continue;
      }
      // Row was removed. Clear every child that used to live at this index.
      Object.keys(prevCells).forEach((child) => {
        keys.push(`${name}.${index}.${child}`);
      });
    }
  });
  return keys;
};

/**
 * Refresh errors for the fields just edited.
 * A message that is already showing stays in sync and clears when the value becomes valid.
 * A new message is added on focus out, not on the first keystroke.
 * Fields the citizen has not edited keep the message they already have.
 *
 * @param {object}   prevErrors
 * @param {object}   freshErrors Result of validateFields for the updated form.
 * @param {string[]} [touchedKeys]
 * @param {string[]} [prefixes]
 * @returns {object}
 */
const refreshTouchedErrors = (prevErrors, freshErrors, touchedKeys = [], prefixes = []) => {
  const keys = new Set(touchedKeys.filter(Boolean));
  const next = { ...prevErrors };
  const matches = (key) => isTouchedErrorKey(key, keys, prefixes);
  const previousTouched = Object.keys(next).filter(matches);
  const hadPrefixErrors = prefixes.some((prefix) =>
    previousTouched.some((key) => key === prefix || key.startsWith(`${prefix}.`))
  );

  previousTouched.forEach((key) => {
    delete next[key];
  });

  previousTouched.forEach((key) => {
    if (freshErrors?.[key]) next[key] = freshErrors[key];
  });

  if (hadPrefixErrors) {
    Object.keys(freshErrors || {}).forEach((key) => {
      if (
        freshErrors[key] &&
        prefixes.some((prefix) => key === prefix || key.startsWith(`${prefix}.`))
      ) {
        next[key] = freshErrors[key];
      }
    });
  }

  return next;
};

/**
 * Replace errors that belong to one accordion section with a fresh validation result.
 *
 * @param {object}        prevErrors
 * @param {Set<string>}   names
 * @param {object}        fieldErrors
 * @returns {object}
 */
const replaceSectionErrors = (prevErrors, names, fieldErrors) => {
  const next = { ...prevErrors };
  Object.keys(next).forEach((key) => {
    if (errorKeyBelongsTo(key, names)) delete next[key];
  });
  Object.keys(fieldErrors || {}).forEach((key) => {
    if (fieldErrors[key]) next[key] = fieldErrors[key];
  });
  return next;
};

/**
 * Config-driven form engine for wizard steps and search filter pages.
 * Loads MDMS options, hydrates/validates form state, runs computed fields,
 * and builds create / edit / search payloads.
 *
 * @param {object} props — see file-level Props section above.
 * @returns {JSX.Element}
 */
const DynamicForm = ({
  routeConfig,
  onSubmit,
  onSelect,
  config,
  isEditMode = false,
  isDisabled = false,
  updateMutation,
  editData = {},
  tenantId = "",
  persistedData = {},
  t = (k) => k,
  showCancel = true,
  cancelLabel = "CS_COMMON_CANCEL",
  confirmCancel = false,
  cancelConfirmHeading = "CS_COMMON_CANCEL",
  cancelConfirmMessage = "CS_WANT_TO_CANCEL",
  cancelConfirmYesLabel = "CS_COMMON_YES",
  cancelConfirmNoLabel = "CS_COMMON_NO",
  onCancel,
  resetBaseline,
  showDraftButton = false,
  draftLabel = "CS_COMMON_SAVE_DRAFT",
  draftSuccessLabel = "CS_COMMON_SAVED",
  onSaveDraft,
  onPersistDraft,
  onFieldSearch,
  onPrefillApplied,
  mode = "wizard",
  searchLayout,
  layout,
  sections = [],
  enrichFieldArrayRow = null,
  transformDropdownData = null,
  onFieldChange = null,
  /** (data, changedField) => nextData. Module hook for copied owner fields. */
  reviseFormData = null,
  onTimelineStep = null,
}) => {
  /** True when mode === "search" (filter UI, no wizard ActionBar). */
  const isSearchMode = mode === "search";
  const { pathname } = useLocation();
  const [activeSection, setActiveSection] = useState(null);
  const [activeField, setActiveField] = useState(null);
  const accordionConfig = routeConfig?.navigation?.accordion || {};
  const isAccordionLayout =
    !isSearchMode &&
    Array.isArray(sections) &&
    sections.length > 0 &&
    (layout === "accordion" || routeConfig?.navigation?.pattern === "accordion");
  /** Effective search layout: prop → routeConfig.searchLayout → inline (search) / stack. */
  const resolvedSearchLayout =
    searchLayout || routeConfig?.searchLayout || (isSearchMode ? "inline" : "stack");
  /** Inline search renders fields in a SearchForm row with submit-as-button. */
  const isInlineSearch = isSearchMode && resolvedSearchLayout === "inline";
  const stateId = Digit.ULBService.getStateId();
  /** Wizard / API array key under which form values are stored (default "Assets"). */
  const payloadKey = routeConfig.payloadKey || "Assets";

  /**
   * City tenant used to fetch localities (e.g. pg.citya).
   * Starts as current tenant; updates when the form city field changes.
   */
  const [cityForLocality, setCityForLocality] = useState(() => String(tenantId || "").trim());

  /**
   * MDMS / master dropdown options for all fields in routeConfig.form.
   * Localities are scoped to cityForLocality (selected city), not always tenantId.
   * Module-level `transformDropdownData` (e.g. TL billing-slab filter) may
   * narrow lists after formData is available.
   */
  const { dropdownData: mdmsDropdownData, isLoading } = useDynamicMDMS(
    routeConfig.form,
    stateId,
    tenantId,
    t,
    {
      city: cityForLocality || tenantId,
    }
  );

  /**
   * Raw pre-fill source for buildInitialData / cancel reset.
   * Merges editData with the first persisted step item under
   * persistedData[config.key][payloadKey]; prefers persisted when present.
   *
   * @returns {object} Flat-ish record of known field values.
   */
  const rawAsset = useMemo(() => {
    const saved = persistedData?.[config?.key]?.[payloadKey];
    const persisted = (Array.isArray(saved) ? saved[0] : saved) || {};
    if (Object.keys(persisted).length > 0) return { ...editData, ...persisted };
    return editData && Object.keys(editData).length > 0 ? editData : {};
  }, [editData, persistedData, config, payloadKey]);

  /**
   * Hydrated form snapshot from config + rawAsset + live dropdownData.
   * Dropdown codes are resolved to option objects where possible.
   * Uses unfiltered MDMS options so structure-gated filters don't block prefill.
   *
   * @returns {object} Initial formData shape keyed by field.name.
   */
  const initialData = useMemo(
    () => buildInitialData(routeConfig.form, rawAsset, mdmsDropdownData, tenantId),
    [routeConfig.form, rawAsset, mdmsDropdownData, tenantId]
  );

  const [formData, setFormData] = useState(initialData);
  /** Latest form values for change handlers. Updated on render and on each edit. */
  const formDataRef = useRef(formData);
  formDataRef.current = formData;

  /**
   * Render-time dropdown map. Optional module transform (TL billing slabs, …)
   * can depend on formData (e.g. buildingType / vehicleType).
   */
  const dropdownData = useMemo(() => {
    if (typeof transformDropdownData !== "function") return mdmsDropdownData;
    try {
      return transformDropdownData(mdmsDropdownData, formData) || mdmsDropdownData;
    } catch (_err) {
      return mdmsDropdownData;
    }
  }, [mdmsDropdownData, formData, transformDropdownData]);
  const [errors, setErrors] = useState({});
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [crossFieldMessages, setCrossFieldMessages] = useState([]);
  /** @type {[{ message: string, error: boolean }] | null} */
  const [toast, setToast] = useState(null);

  useEffect(() => {
    if (toast) {
      const timer = setTimeout(() => {
        setToast(null);
      }, 2000);
      return () => clearTimeout(timer);
    }
  }, [toast]);

  const [isFieldSearching, setIsFieldSearching] = useState(false);
  /**
   * Asset / estate lookup panel state.
   * null | { fieldName, status: "found"|"notFound"|"matches", estateNo, prefill?, matches? }
   */
  const [searchPanel, setSearchPanel] = useState(null);
  /** After picking a suggestion, hide typeahead until the user types again. */
  const suppressSuggestRef = useRef(false);
  /** Wizard Cancel confirmation modal (reuses shared Modal / confirmation_box). */
  const [showCancelPopup, setShowCancelPopup] = useState(false);
  /** Accordion panel to expand after validation errors (submit / continue). */
  const [accordionFocusId, setAccordionFocusId] = useState(null);
  /** Create-new redirect confirm — holds target path when open. */
  const [createNewRedirectPath, setCreateNewRedirectPath] = useState(null);

  /**
   * Keeps locality MDMS bound to the city currently on the form.
   * Reads formData.city as option object (.code) or string; preserves original
   * tenant casing (MDMS uses pg.citya, not PG.CITYA).
   */
  useEffect(() => {
    const city = formData?.city;
    const fromForm =
      (city && typeof city === "object" && city.code != null
        ? String(city.code).trim()
        : "") || (typeof city === "string" ? city.trim() : "");
    const next = String(fromForm || tenantId || "").trim();
    if (next && next !== cityForLocality) {
      setCityForLocality(next);
    }
  }, [formData?.city, tenantId, cityForLocality]);

  /**
   * Flattened leaf field configs (groups expanded) — used for compute / watch / gates.
   * @returns {object[]}
   */
  const flatFields = useMemo(() => flattenFormConfig(routeConfig.form || []), [routeConfig.form]);

  /**
   * Optional top-of-form gate (Estate asset / TL property). Null when ungated.
   * @returns {object|null}
   */
  const formGate = useMemo(
    () => resolveFormGate(routeConfig, flatFields),
    [routeConfig, flatFields]
  );

  /**
   * Top-level form entries sorted by order for render.
   * @returns {object[]}
   */
  const sortedFields = useMemo(() => sortByOrder(routeConfig.form || []), [routeConfig.form]);

  /**
   * Options for the billingCycle field (MDMS dropdown, key lookup, or static options).
   * Needed so computeFns receive enriched option objects with multipliers.
   *
   * @returns {object[]}
   */
  const billingCycleOptions = useMemo(() => {
    const billingField = findFieldConfig(routeConfig.form, "billingCycle");
    return (
      dropdownData?.billingCycle ||
      dropdownData?.[billingField?.key] ||
      billingField?.options ||
      []
    );
  }, [dropdownData, routeConfig.form]);

  /**
   * Maps a field's computeFrom dependency names to argument values for a computeFn.
   * Special-cases billingCycle so the arg is an enriched dropdown option.
   *
   * @param {string[]} computeFrom - Dependency field names.
   * @param {string}   computeFn   - Registry key (unused except for call sites).
   * @param {object}   data        - Current form data snapshot.
   * @returns {*[]} Positional args for COMPUTE_REGISTRY[computeFn].
   */
  const resolveComputeArgs = useCallback(
    (computeFrom, computeFn, data) =>
      computeFrom.map((dep) => {
        const val = data[dep];
        if (dep === "billingCycle") {
          return enrichDropdownSelection(val, billingCycleOptions);
        }
        return val;
      }),
    [billingCycleOptions]
  );

  /**
   * Re-derives any field whose computeFrom list intersects `changedFieldNames`.
   * Runs iterative rounds (max 10) so cascading computes (A→B→C) settle.
   * Looks up each field.computeFn in COMPUTE_REGISTRY; calculateRentByBillingCycle
   * also receives billingCycleOptions as a trailing argument.
   *
   * @param {object}   updated           Form data after the triggering change.
   * @param {string[]} changedFieldNames Field names that just changed (seeds the queue).
   * @returns {object} Form data with computed fields updated.
   */
  const applyComputedFields = useCallback(
    (updated, changedFieldNames) => {
      let next = updated;
      const pending = new Set(changedFieldNames);
      let iterations = 0;

      while (pending.size > 0 && iterations < 10) {
        iterations += 1;
        const round = [...pending];
        pending.clear();

        flatFields.forEach((fc) => {
          const { computeFrom, computeFn, name } = fc.field || {};
          if (!computeFrom || !computeFn) return;
          if (!computeFrom.some((dep) => round.includes(dep))) return;

          const fn = COMPUTE_REGISTRY[computeFn];
          if (!fn) return;

          const args = resolveComputeArgs(computeFrom, computeFn, next);
          const newVal =
            computeFn === "calculateRentByBillingCycle"
              ? fn(...args, billingCycleOptions)
              : fn(...args);
          if (next[name] !== newVal) {
            next = { ...next, [name]: newVal };
            pending.add(name);
          }
        });
      }

      return next;
    },
    [flatFields, billingCycleOptions, resolveComputeArgs]
  );

  /**
   * Guards the one-time post-MDMS form sync so it never re-fires when
   * initialData's identity changes after the first load.
   */
  const hasSyncedRef = useRef(false);

  /**
   * Syncs formData with initialData EXACTLY ONCE after dropdownData finishes loading.
   * Fresh initialData wins over the pre-load snapshot (real options replace placeholders);
   * non-empty user-typed values are preserved on top. Also auto-sets
   * showRegistrationDetails when edit / building / NEW_BUILDING imply it, then
   * runs applyComputedFields for all compute dependencies.
   */
  useEffect(() => {
    if (hasSyncedRef.current || isLoading) return;
    hasSyncedRef.current = true;
    setFormData((prev) => {
      const userTouched = Object.fromEntries(
        Object.entries(prev).filter(([, v]) => v !== "" && v !== null && v !== undefined)
      );
      const merged = { ...initialData, ...userTouched };
      if (formGate) {
        const detailsKey = formGate.detailsField;
        const typeVal = String(merged[formGate.typeField] || "").toUpperCase();
        const openVal = formGate.openValue
          ? String(formGate.openValue).toUpperCase()
          : null;
        const hintFields = formGate.unlockWhenFields || [];
        const hasHint = hintFields.some((key) => {
          const value = merged[key];
          return value !== undefined && value !== null && String(value).trim() !== "";
        });
        if (
          !merged[detailsKey] &&
          (isEditMode || hasHint || (openVal && typeVal === openVal))
        ) {
          merged[detailsKey] = "YES";
        }
      }
      const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
      return allComputeDeps.length ? applyComputedFields(merged, allComputeDeps) : merged;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading]);

  /**
   * Merges a lookup/search prefill into form state via buildInitialData, then
   * overlays `preserve` keys (registration type, estate no, gates) and re-runs
   * computed fields.
   *
   * @param {object} [prefill]   Values from asset/estate search to apply.
   * @param {object} [preserve]  Keys that must win after buildInitialData.
   */
  const applyPrefill = useCallback(
    (prefill = {}, preserve = {}) => {
      setFormData((prev) => {
        const source = { ...prev, ...prefill };
        const built = buildInitialData(
          routeConfig.form,
          source,
          dropdownData,
          tenantId
        );
        // Keep stash keys (e.g. __property) that are not form fields —
        // buildInitialData only copies configured field names.
        const stash = {};
        Object.keys(source).forEach((key) => {
          if (key.startsWith("__")) stash[key] = source[key];
        });
        const kept = {};
        (formGate?.preserveOnPrefill || []).forEach((key) => {
          if (preserve[key] !== undefined) kept[key] = preserve[key];
          else if (source[key] !== undefined) kept[key] = source[key];
          else if (built[key] !== undefined) kept[key] = built[key];
        });
        let merged = {
          ...built,
          ...stash,
          ...preserve,
          ...kept,
        };
        if (typeof reviseFormData === "function") {
          const revised = reviseFormData(merged, null);
          if (revised && typeof revised === "object") merged = revised;
        }
        const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
        return allComputeDeps.length
          ? applyComputedFields(merged, allComputeDeps)
          : merged;
      });
    },
    [routeConfig.form, dropdownData, tenantId, flatFields, applyComputedFields, formGate, reviseFormData]
  );

  /**
   * Primary field onChange from DynamicFormField.
   * Special cases:
   * - formGate.typeField (Estate asset / TL property) → wipe baseline + gate flags,
   *   or open createNewPath redirect when redirectOnValue is selected.
   * - billingCycle → enrichDropdownSelection so multipliers stay on the value.
   * - city → update cityForLocality and clear serviceType.
   * Re-validates the changed field (and field-array keys) so a fixed error
   * drops out while other fields keep their current messages.
   *
   * @param {string}   fieldName     field.name that changed.
   * @param {*}        value         New value (string, option object, file meta, etc.).
   * @param {string[]} [resetFields] Sibling fields to null out (cascading clears).
   * @param {object}   [meta]
   * @param {string[]} [meta.clearErrorKeys] Exact error keys to refresh (field-array cells).
   * @param {string[]} [meta.refreshPrefixes] Error-key prefixes to refresh (row add/remove).
   */
  const notifyFieldChange = (name, nextValue) => {
    if (name) setActiveField(name);
    if (typeof onFieldChange !== "function") return;
    try {
      onFieldChange(name, nextValue);
    } catch (err) {
      console.warn("onFieldChange failed", err);
    }
  };

  const assignResetValue = (updated, name) => {
    const arrayItem = (routeConfig.form || []).find(
      (item) => item?.type === "fieldArray" && getFieldArrayName(item) === name
    );
    if (arrayItem) {
      const minItems = Math.max(1, Number(arrayItem.minItems) || 1);
      updated[name] = Array.from({ length: minItems }, () =>
        createEmptyFieldArrayItem(arrayItem.children)
      );
      return;
    }
    updated[name] = null;
  };

  const handleChange = useCallback(
    (fieldName, value, resetFields = [], meta = {}) => {
      if (formGate && fieldName === formGate.typeField) {
        suppressSuggestRef.current = false;
        setSearchPanel(null);
        setErrors({});
        setCrossFieldMessages([]);

        const normalized = String(value || "").toUpperCase();
        const openVal = formGate.openValue
          ? String(formGate.openValue).toUpperCase()
          : null;
        const searchVal = String(formGate.searchValue || "").toUpperCase();
        const redirectVal = formGate.redirectOnValue
          ? String(formGate.redirectOnValue).toUpperCase()
          : null;

        if (redirectVal && normalized === redirectVal) {
          const path =
            formGate.redirectPath ||
            resolveCreateNewPath(flatFields, formGate.searchField);
          if (path) {
            setCreateNewRedirectPath(path);
          }
          setFormData((prev) => {
            const blank = buildInitialData(routeConfig.form, {}, dropdownData, tenantId);
            const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
            const kept = {};
            (formGate.preserveOnPrefill || []).forEach((key) => {
              if (prev[key] !== undefined) kept[key] = prev[key];
            });
            const cleared = {
              ...blank,
              ...kept,
              [formGate.typeField]: value,
              [formGate.searchField]: "",
              [formGate.detailsField]: "",
            };
            return allComputeDeps.length
              ? applyComputedFields(cleared, allComputeDeps)
              : cleared;
          });
          notifyFieldChange(fieldName, value);
          return;
        }

        if (openVal && normalized === openVal) {
          setFormData((prev) => {
            const blank = buildInitialData(routeConfig.form, {}, dropdownData, tenantId);
            const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
            const kept = {};
            (formGate.preserveOnPrefill || []).forEach((key) => {
              if (prev[key] !== undefined) kept[key] = prev[key];
            });
            const cleared = {
              ...blank,
              ...kept,
              [formGate.typeField]: value,
              [formGate.searchField]: "",
              [formGate.detailsField]: "YES",
            };
            return allComputeDeps.length
              ? applyComputedFields(cleared, allComputeDeps)
              : cleared;
          });
          notifyFieldChange(fieldName, value);
          return;
        }

        if (normalized === searchVal) {
          setFormData((prev) => {
            const blank = buildInitialData(routeConfig.form, {}, dropdownData, tenantId);
            const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
            const kept = {};
            (formGate.preserveOnPrefill || []).forEach((key) => {
              if (prev[key] !== undefined) kept[key] = prev[key];
            });
            const cleared = {
              ...blank,
              ...kept,
              [formGate.typeField]: value,
              [formGate.searchField]: "",
              [formGate.detailsField]: "",
            };
            return allComputeDeps.length
              ? applyComputedFields(cleared, allComputeDeps)
              : cleared;
          });
          notifyFieldChange(fieldName, value);
          return;
        }
      }

      const structureReset = formGate?.structureReset;
      if (structureReset?.field && fieldName === structureReset.field) {
        suppressSuggestRef.current = false;
        setSearchPanel(null);
        setErrors({});
        setCrossFieldMessages([]);
        const normalized = optionCode(value);
        const unlock = String(structureReset.unlockValue || "").toUpperCase();
        const blank = buildInitialData(routeConfig.form, {}, dropdownData, tenantId);
        const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
        const cleared = {
          ...blank,
          [structureReset.field]: typeof value === "string" ? value : normalized,
        };
        (structureReset.clearFields || []).forEach((name) => {
          cleared[name] = "";
        });
        if (formGate.detailsField) {
          cleared[formGate.detailsField] = normalized === unlock ? "YES" : "";
        }
        setFormData(
          allComputeDeps.length
            ? applyComputedFields(cleared, allComputeDeps)
            : cleared
        );
        notifyFieldChange(fieldName, value);
        return;
      }

      const resetList = resetFields || [];
      let resolvedValue = value;
      if (fieldName === "billingCycle") {
        resolvedValue = enrichDropdownSelection(value, billingCycleOptions);
      }
      const previousData = formDataRef.current;
      let updated = { ...previousData, [fieldName]: resolvedValue };
      resetList.forEach((f) => {
        assignResetValue(updated, f);
      });

      // City change → clear locality; localities will reload for the new city.
      if (fieldName === "city") {
        const nextCity =
          optionCode(resolvedValue) ||
          (typeof resolvedValue === "string" ? resolvedValue : "");
        if (nextCity) setCityForLocality(String(nextCity).trim());
        updated.serviceType = null;
        updated.serviceTypeName = "";
      }

      // Seed fieldArray rows when a gate radio unlocks them (e.g. accessories).
      (routeConfig.form || []).forEach((item) => {
        if (item?.type !== "fieldArray") return;
        const arrayName = item.field?.name;
        if (!arrayName) return;
        const rule = item.visibleWhen;
        if (!rule?.field || rule.field !== fieldName) return;
        const show =
          rule.equals !== undefined
            ? String(resolvedValue || "").toUpperCase() ===
              String(rule.equals).toUpperCase()
            : Array.isArray(rule.in) &&
              rule.in
                .map((v) => String(v).toUpperCase())
                .includes(String(resolvedValue || "").toUpperCase());
        if (!show) return;
        if (!Array.isArray(updated[arrayName]) || updated[arrayName].length === 0) {
          const minItems = Math.max(1, Number(item.minItems) || 1);
          // One fresh row object per slot so later edits do not share state.
          updated[arrayName] = Array.from({ length: minItems }, () =>
            createEmptyFieldArrayItem(item.children)
          );
        }
      });

      // Module hook (trade license copies owner address / radios here).
      if (typeof reviseFormData === "function") {
        const revised = reviseFormData(updated, fieldName);
        if (revised && typeof revised === "object") updated = revised;
      }

      const nextData = applyComputedFields(updated, [fieldName, ...resetList]);
      formDataRef.current = nextData;
      setFormData(nextData);
      notifyFieldChange(fieldName, value);
      if (formGate && fieldName === formGate.searchField) {
        suppressSuggestRef.current = false;
      }
      const clearErrorKeys = Array.isArray(meta?.clearErrorKeys) ? meta.clearErrorKeys : [];
      const refreshPrefixes = Array.isArray(meta?.refreshPrefixes) ? meta.refreshPrefixes : [];
      const touched = [
        fieldName,
        ...resetList,
        ...clearErrorKeys,
        ...changedArrayCellKeys(previousData, nextData),
      ];
      if (fieldName === "city") touched.push("serviceType");
      const resized = resizedArrayNames(previousData, nextData);
      setErrors((prev) =>
        refreshTouchedErrors(
          dropErrorPrefixes(prev, resized),
          validateFields(routeConfig.form, nextData),
          touched,
          refreshPrefixes
        )
      );
    },
    [
      applyComputedFields,
      billingCycleOptions,
      routeConfig.form,
      dropdownData,
      tenantId,
      flatFields,
      formGate,
      onFieldChange,
      reviseFormData,
    ]
  );

  /**
   * Validate one field when focus leaves it. The message is for that field only.
   * @param {string} errorKey Field name, or `array.index.child` for a repeated row.
   */
  const handleFieldBlur = useCallback((errorKey) => {
    if (!errorKey) return;
    const fresh = validateFields(routeConfig.form, formDataRef.current);
    setErrors((prev) => {
      const next = { ...prev };
      if (fresh[errorKey]) next[errorKey] = fresh[errorKey];
      else delete next[errorKey];
      return next;
    });
  }, [routeConfig.form]);

  /**
   * Runs the parent onFieldSearch for a lookup field (Enter / search button / typeahead).
   * Maps the result into searchPanel status: matches | found | notFound, or an error toast.
   * Empty query clears the panel; if the user triggered search (no queryOverride), marks the field invalid.
   *
   * @param {string}  fieldName       Field that owns the search (e.g. searchEstateNo).
   * @param {string}  [queryOverride] Optional query (typeahead); else formData[fieldName].
   * @returns {Promise<void>}
   */
  const handleFieldSearch = useCallback(
    async (fieldName, queryOverride) => {
      if (!onFieldSearch) return;
      const query = String(
        queryOverride !== undefined ? queryOverride : formData[fieldName] || ""
      ).trim();
      if (!query) {
        setSearchPanel(null);
        if (queryOverride === undefined) {
          setErrors((prev) => ({ ...prev, [fieldName]: true }));
        }
        return;
      }

      setIsFieldSearching(true);
      try {
        const result = await onFieldSearch(fieldName, {
          ...formData,
          [fieldName]: query,
        });
        if (result?.error && !result?.notFound) {
          setToast({ message: t(result.error), error: true });
          return;
        }
        if (
          result?.notFound ||
          (formGate?.notFoundError && result?.error === formGate.notFoundError)
        ) {
          setSearchPanel({
            fieldName,
            status: "notFound",
            estateNo: query,
          });
          return;
        }
        if (Array.isArray(result?.matches) && result.matches.length > 0) {
          setSearchPanel({
            fieldName,
            status: "matches",
            estateNo: query,
            matches: result.matches,
          });
          return;
        }
        if (result?.found || result?.prefill) {
          // Auto-apply single hit so gated detail fields unlock without an extra click.
          const estateNo = result.estateNo || query;
          const prefill = result.prefill || {};
          suppressSuggestRef.current = true;
          setSearchPanel(null);
          const preserve = formGate
            ? {
                [formGate.typeField]: formGate.searchValue,
                [formGate.searchField]: estateNo,
                [formGate.detailsField]: "YES",
              }
            : { [fieldName]: estateNo };
          applyPrefill(prefill, preserve);
          onPrefillApplied?.(prefill);
          setToast({ message: t("CS_COMMON_RECORD_FOUND"), error: false });
          return;
        }
        setSearchPanel({
          fieldName,
          status: "notFound",
          estateNo: query,
        });
      } catch (err) {
        console.error("Field search failed:", err);
        setToast({ message: t("CS_SOMETHING_WENT_WRONG"), error: true });
      } finally {
        setIsFieldSearching(false);
      }
    },
    [onFieldSearch, formData, t, formGate, applyPrefill, onPrefillApplied]
  );

  /**
   * Optional live typeahead for the gate search field when field.searchTypeahead === true
   * and the gate is in search mode. Debounces 250ms; requires ≥3 chars.
   * Off by default — estate / property search normally uses exact match on Enter/button.
   */
  useEffect(() => {
    if (!formGate) return undefined;
    if (
      String(formData[formGate.typeField] || "").toUpperCase() !==
      String(formGate.searchValue || "").toUpperCase()
    ) {
      return undefined;
    }
    const searchField = flatFields.find((fc) => fc?.field?.name === formGate.searchField);
    if (searchField?.field?.searchTypeahead !== true) {
      return undefined;
    }
    if (suppressSuggestRef.current) {
      return undefined;
    }
    const query = String(formData[formGate.searchField] || "").trim();
    if (query.length < 3) {
      setSearchPanel((prev) =>
        prev?.fieldName === formGate.searchField ? null : prev
      );
      return undefined;
    }
    const timer = setTimeout(() => {
      if (suppressSuggestRef.current) return;
      handleFieldSearch(formGate.searchField, query);
    }, 250);
    return () => clearTimeout(timer);
  }, [
    formGate,
    formData?.[formGate?.typeField],
    formData?.[formGate?.searchField],
    handleFieldSearch,
    flatFields,
  ]);

  /**
   * User picked a match from the suggestion list or the "found" result card.
   * Prefills form via applyPrefill, locks the gate to search mode, and
   * suppresses further typeahead until the search field is edited again.
   *
   * @param {string} fieldName - Lookup field name.
   * @param {object} [match]   - { estateNo, prefill } from suggestions; else uses searchPanel.
   */
  const handleSelectSearchResult = useCallback(
    (fieldName, match) => {
      const selected =
        match ||
        (searchPanel?.fieldName === fieldName && searchPanel.status === "found"
          ? {
              estateNo: searchPanel.estateNo,
              prefill: searchPanel.prefill || {},
            }
          : null);
      if (!selected) return;

      suppressSuggestRef.current = true;
      setSearchPanel(null);
      const preserve = formGate
        ? {
            [formGate.typeField]: formGate.searchValue,
            [formGate.searchField]: selected.estateNo || formData[fieldName],
            [formGate.detailsField]: "YES",
          }
        : { [fieldName]: selected.estateNo || formData[fieldName] };
      applyPrefill(selected.prefill || {}, preserve);
      onPrefillApplied?.(selected.prefill || {});
      setToast({ message: t("CS_COMMON_RECORD_FOUND"), error: false });
    },
    [searchPanel, applyPrefill, formData, t, formGate, onPrefillApplied]
  );

  /**
   * "Create new" from a not-found search panel.
   * When the field defines createNewPath, opens a confirmation modal before navigate.
   * Otherwise, if the gate has openValue, resets to that baseline with details shown.
   */
  const handleCreateNewFromSearch = useCallback(
    (fieldName) => {
      const createNewPath =
        flatFields.find((fc) => fc.field?.name === fieldName)?.field?.createNewPath ||
        resolveCreateNewPath(flatFields, formGate?.searchField);
      if (createNewPath) {
        setCreateNewRedirectPath(createNewPath);
        return;
      }

      if (!formGate?.openValue) return;

      const blank = buildInitialData(routeConfig.form, {}, dropdownData, tenantId);
      const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
      const cleared = {
        ...blank,
        [formGate.typeField]: formGate.openValue,
        [formGate.searchField]: "",
        [formGate.detailsField]: "YES",
      };
      setFormData(
        allComputeDeps.length
          ? applyComputedFields(cleared, allComputeDeps)
          : cleared
      );
      setErrors({});
      setCrossFieldMessages([]);
      setSearchPanel(null);
    },
    [
      routeConfig.form,
      dropdownData,
      tenantId,
      flatFields,
      applyComputedFields,
      formGate,
    ]
  );

  /**
   * Debounced auto-draft: when onPersistDraft is set and no explicit draft button
   * is shown, persists buildPayload(formData) 400ms after formData changes
   * (only after the initial MDMS sync has completed).
   */
  useEffect(() => {
    if (!onPersistDraft || showDraftButton || isLoading || !hasSyncedRef.current) return;

    const timer = setTimeout(() => {
      onPersistDraft(buildPayload(formData));
    }, 400);

    return () => clearTimeout(timer);
  }, [formData, onPersistDraft, showDraftButton, isLoading]);

  /**
   * Uploads a file via Digit.UploadServices.Filestorage and stores
   * { filestoreId, documentuuid, documentType, fileName } on the field.
   * Rejects files ≥ 5MB with a toast. Module defaults to "ESTATE";
   * override with routeConfig.uploadModule.
   *
   * @param {string} fieldName - File field name.
   * @param {File}   file      - Browser File from UploadFile.
   * @returns {Promise<void>}
   */
  const handleFileUpload = useCallback(
    async (fieldName, file) => {
      if (!file) return;
      if (file.size >= 5242880) {
        setToast({ message: t("CS_MAXIMUM_UPLOAD_SIZE_EXCEEDED"), error: true });
        return;
      }
      try {
        const response = await Digit.UploadServices.Filestorage(
          routeConfig.uploadModule || "ESTATE",
          file,
          tenantId
        );
        const id = response?.data?.files?.[0]?.fileStoreId;
        if (id) {
          handleChange(fieldName, {
            filestoreId: id,
            documentuuid: id,
            documentType: fieldName,
            fileName: file.name || "",
          });
        }
      } catch (err) {
        console.error("File upload failed:", err?.response?.data || err);
        setToast({ message: t("CS_FILE_UPLOAD_ERROR"), error: true });
      }
    },
    [routeConfig.uploadModule, tenantId, t, handleChange]
  );

  /**
   * Primary submit / Save & Next / Search / Update handler.
   * Wizard: validates fields + crossFieldValidations; on success buildPayload then
   *   - create → onSelect(config.key, { [payloadKey]: [formVal] }) + onSubmit
   *   - edit   → updateMutation.mutate(...) then onSubmit with response/error
   * Search: skips wizard validation; maps filters via mapFormToSearchFilters → onSubmit.
   */
  const goNext = useCallback(() => {
    if (!isSearchMode) {
      const fieldErrors = validateFields(routeConfig.form, formData);
      const { errors: crossErrors, failures } = validateCrossField(routeConfig.crossFieldValidations, formData);
      const allErrors = { ...fieldErrors, ...crossErrors };

      if (Object.keys(allErrors).length > 0) {
        setErrors(allErrors);
        setCrossFieldMessages(failures.map((f) => f.message));
        if (isAccordionLayout) {
          setAccordionFocusId(
            findAccordionSectionId(sections, allErrors) || sections[0]?.id || null
          );
        }
        scrollToFirstError();
        return;
      }
      setCrossFieldMessages([]);
    }

    const formVal = buildPayload(formData);

    if (isSearchMode) {
      const filters = mapFormToSearchFilters(formVal, routeConfig.form);
      onSubmit?.({ payload: { ...filters, offset: 0 }, formValues: formVal, isSearch: true });
      return;
    }

    const payload = { [payloadKey]: [formVal], tenantId };

    if (isEditMode) {
      if (!updateMutation) {
        console.error("updateMutation is required in edit mode");
        return;
      }
      setIsSubmitting(true);
      const updatePayload = {
        [payloadKey]: { ...formVal, id: editData.id, tenantId, ...routeConfig.editPayloadExtras?.(editData) },
      };
      updateMutation.mutate(updatePayload, {
        onSuccess: (data) => {
          setIsSubmitting(false);
          onSubmit && onSubmit({ payload, response: data, isEditMode: true });
        },
        onError: (error) => {
          setIsSubmitting(false);
          setToast({ message: t("CS_COMMON_UPDATE_FAILED"), error: true });
          console.error("Update failed:", error);
          onSubmit && onSubmit({ payload, error, isEditMode: true });
        },
      });
    } else {
      onSelect && onSelect(config?.key, { [payloadKey]: [formVal] }, false);
      onSubmit && onSubmit({ payload, isEditMode: false });
    }
  }, [
    isSearchMode,
    isAccordionLayout,
    sections,
    routeConfig,
    formData,
    isEditMode,
    updateMutation,
    editData,
    tenantId,
    onSelect,
    onSubmit,
    config,
    payloadKey,
    t,
  ]);

  /**
   * Cancel / Clear All: rebuilds form from resetBaseline (preferred) or rawAsset.
   * Skips field.defaultValue / prefillFrom so cancel leaves editable fields blank
   * (e.g. advancePaymentDate "today" is not re-applied).
   * An explicit empty resetBaseline `{}` clears everything (does not fall back to rawAsset).
   */
  const handleCancel = useCallback(() => {
    const baselineSource =
      resetBaseline !== undefined && resetBaseline !== null
        ? resetBaseline
        : rawAsset;
    const resetData = buildInitialData(
      routeConfig.form,
      baselineSource,
      dropdownData,
      tenantId,
      { applyDefaults: false }
    );
    const allComputeDeps = flatFields.flatMap((fc) => fc.field?.computeFrom || []);
    const next = allComputeDeps.length
      ? applyComputedFields({ ...resetData }, allComputeDeps)
      : { ...resetData };
    setFormData(next);
    setErrors({});
    setCrossFieldMessages([]);
    setSearchPanel(null);
    setShowCancelPopup(false);
    onCancel?.();
  }, [
    routeConfig.form,
    resetBaseline,
    rawAsset,
    dropdownData,
    tenantId,
    flatFields,
    applyComputedFields,
    onCancel,
  ]);

  /**
   * Wizard Cancel click — show confirmation Modal first when confirmCancel is on;
   * search Clear All still resets immediately via handleCancel.
   */
  const requestCancel = useCallback(() => {
    if (confirmCancel && !isSearchMode) {
      setShowCancelPopup(true);
      return;
    }
    handleCancel();
  }, [confirmCancel, isSearchMode, handleCancel]);

  const dismissCancelPopup = useCallback(() => setShowCancelPopup(false), []);

  /**
   * Explicit Save Draft button handler.
   * Passes buildPayload(formData) to onSaveDraft and shows a success toast.
   */
  const handleSaveDraft = useCallback(() => {
    if (!onSaveDraft) return;
    onSaveDraft(buildPayload(formData));
    setToast({ message: t(draftSuccessLabel), error: false });
  }, [onSaveDraft, formData, draftSuccessLabel, t]);

  /**
   * Accordion "Save & continue": validate only this panel's fields.
   * @param {{ id: string, form: object[] }} section
   * @returns {boolean} false when the panel has errors.
   */
  const handleSectionContinue = useCallback(
    (section) => {
      const fieldErrors = validateFields(section?.form, formData);
      const names = collectSectionFieldNames(section?.form);
      setErrors((prev) => replaceSectionErrors(prev, names, fieldErrors));
      if (Object.keys(fieldErrors).length > 0) {
        setAccordionFocusId(section?.id || null);
        scrollToFirstError();
        return false;
      }
      return true;
    },
    [formData]
  );

  /** Show every invalid field in the section that is keeping a later panel closed. */
  const handleSectionBlocked = useCallback((section) => {
    if (!section?.form) return;
    const fieldErrors = validateFields(section.form, formDataRef.current);
    const names = collectSectionFieldNames(section.form);
    setErrors((prev) => replaceSectionErrors(prev, names, fieldErrors));
    setAccordionFocusId(section.id || null);
    scrollToFirstError();
  }, []);

  /** Primary action label: Update (edit) / Search (search mode) / SAVE & NEXT (create). */
  const buttonLabel = isEditMode
    ? routeConfig.actionButton?.text?.edit || "UPDATE"
    : routeConfig.actionButton?.text?.create || (isSearchMode ? "ES_COMMON_SEARCH" : "SAVE & NEXT");

  /** Clear / cancel label for search clear link (and related copy). */
  const clearLabel = cancelLabel || routeConfig.actionButton?.text?.clear || "ES_COMMON_CLEAR_ALL";

  const timelineConfig = routeConfig?.navigation?.timeline;
  useEffect(() => {
    if (typeof onTimelineStep !== "function" || !timelineConfig?.steps?.length) return;
    const parts = String(pathname || "").split("/").filter(Boolean);
    const route = parts[parts.length - 1] || "";
    const step = resolveConfigTimelineStep(timelineConfig, {
      route,
      activeSection,
      activeField,
      values: formData,
    });
    if (step != null) onTimelineStep(step);
  }, [
    onTimelineStep,
    timelineConfig,
    pathname,
    activeSection,
    activeField,
    formData,
  ]);

  // Only block the first paint. Remounting the form on later MDMS/locality
  // refetches wipes in-progress dropdown selections (search filters, etc.).
  if (isLoading && !hasSyncedRef.current) return <Loader />;

  /**
   * True when this form uses a top gate (Estate asset / TL property).
   * Ungated forms always show the ActionBar.
   */
  const usesRegistrationGate = !!formGate;
  /** Show Cancel/Draft/Submit only when ungated or details are ready. */
  const showActionBar =
    !usesRegistrationGate || shouldShowGatedActionBar(formGate, formData);

  /** One DynamicFormField node per form entry (shared by stack and accordion). */
  const renderField = (fieldConfig) => (
    <DynamicFormField
      key={fieldConfig.key}
      fieldConfig={fieldConfig}
      formData={formData}
      onChange={handleChange}
      onFieldBlur={handleFieldBlur}
      errors={errors}
      dropdownData={dropdownData}
      t={t}
      isDisabled={isDisabled}
      onFileUpload={handleFileUpload}
      onFieldSearch={handleFieldSearch}
      isFieldSearching={isFieldSearching}
      searchPanel={searchPanel}
      onSelectSearchResult={handleSelectSearchResult}
      onCreateNewFromSearch={handleCreateNewFromSearch}
      enrichFieldArrayRow={enrichFieldArrayRow}
    />
  );

  const fieldNodes = sortedFields.map(renderField);
  const accordionNodes = isAccordionLayout ? (
    <DynamicFormAccordion
      sections={sections}
      formData={formData}
      errors={errors}
      accordion={accordionConfig}
      t={t}
      isDisabled={isDisabled}
      focusSectionId={accordionFocusId}
      renderFields={(formSlice) =>
        sortByOrder(formSlice)
          // Skip invisible fields so empty grid cells don't leave gaps / overlaps.
          .filter((fieldConfig) => isFieldVisible(fieldConfig, formData))
          .map((fieldConfig) => (
            <div key={fieldConfig.key} className={getAccordionCellClass(fieldConfig)}>
              {renderField(fieldConfig)}
            </div>
          ))
      }
      onContinue={handleSectionContinue}
      onSectionBlocked={handleSectionBlocked}
      onActiveSectionChange={setActiveSection}
    />
  ) : null;

  /** Inline search: SubmitBar (form submit) + clear link inside SearchField. */
  const searchActions = !isDisabled && isSearchMode && (
    <SearchField className="submit">
      <SubmitBar label={t(buttonLabel)} submit />
      <p className="dynamic-form-search-clear" onClick={handleCancel}>
        {t(clearLabel)}
      </p>
    </SearchField>
  );

  /** Stacked search: SubmitBar calling goNext + clear, below the field list. */
  const stackedSearchActions = !isDisabled && isSearchMode && !isInlineSearch && (
    <div className="dynamic-form-search-actions">
      <SubmitBar
        label={t(buttonLabel)}
        onSubmit={goNext}
        className="dynamic-form-search-submit"
      />
      <p className="dynamic-form-search-clear" onClick={handleCancel}>
        {t(clearLabel)}
      </p>
    </div>
  );

  if (isInlineSearch) {
    return (
      <>
        <SearchForm
          onSubmit={goNext}
          handleSubmit={searchFormHandleSubmit}
          className={routeConfig.searchFormClassName || ""}
        >
          {fieldNodes.map((node, i) => (
            <SearchField key={sortedFields[i].key}>{node}</SearchField>
          ))}
          {searchActions}
        </SearchForm>

        {crossFieldMessages.map((msg, i) => (
          <p key={i} className="dynamic-form-error">{t(msg)}</p>
        ))}

        {toast && (
          <Toast label={toast.message} error={toast.error} onClose={() => setToast(null)} />
        )}
      </>
    );
  }

  return (
    <div
      className={`dynamic-form-container${
        isAccordionLayout ? " dynamic-form-container--accordion" : ""
      }`}
    >
      {accordionNodes || fieldNodes}

      {crossFieldMessages.map((msg, i) => (
        <p key={i} className="dynamic-form-error">{t(msg)}</p>
      ))}

      {stackedSearchActions}

      {!isDisabled && !isSearchMode && showActionBar && (
        <ActionBar className="dynamic-form-action">
          <div className="dynamic-form-action-primary">
            <SubmitBar
              label={t(buttonLabel)}
              onSubmit={goNext}
              disabled={isSubmitting}
            />
            {showDraftButton && (
              <ButtonSelector
                theme="border"
                label={t(draftLabel)}
                onSubmit={handleSaveDraft}
              />
            )}
          </div>
          {showCancel && (
            <ButtonSelector
              theme="border"
              label={t(cancelLabel)}
              onSubmit={requestCancel}
            />
          )}
        </ActionBar>
      )}

      {toast && (
        <Toast label={toast.message} error={toast.error} onClose={() => setToast(null)} />
      )}

      {showCancelPopup && (
        <Modal
          headerBarMain={<CancelPopupHeading label={t(cancelConfirmHeading)} />}
          headerBarEnd={<CancelPopupCloseBtn onClick={dismissCancelPopup} />}
          actionCancelLabel={t(cancelConfirmNoLabel)}
          actionCancelOnSubmit={dismissCancelPopup}
          actionSaveLabel={t(cancelConfirmYesLabel)}
          actionSaveOnSubmit={handleCancel}
          formId="modal-action"
        >
          <div className="confirmation_box">
            <CardText>{t(cancelConfirmMessage, "Do you want to cancel?")}</CardText>
          </div>
        </Modal>
      )}

      {createNewRedirectPath && (
        <Modal
          headerBarMain={
            <CancelPopupHeading
              label={
                formGate?.redirectModalHeading
                  ? t(formGate.redirectModalHeading)
                  : formGate?.redirectModalHeadingDefault || ""
              }
            />
          }
          headerBarEnd={
            <CancelPopupCloseBtn onClick={() => setCreateNewRedirectPath(null)} />
          }
          actionCancelLabel={t("CS_COMMON_CANCEL")}
          actionCancelOnSubmit={() => setCreateNewRedirectPath(null)}
          actionSaveLabel={t("CS_COMMON_OK")}
          actionSaveOnSubmit={() => {
            window.location.href = resolveAppPath(createNewRedirectPath);
          }}
          formId="modal-create-new-redirect"
        >
          <div className="confirmation_box">
            <CardText>
              {(() => {
                const msgKey = formGate?.redirectModalMessage || "";
                const msgDefault = formGate?.redirectModalMessageDefault || "";
                const translated = t(msgKey, { defaultValue: msgDefault });
                if (!translated || translated === msgKey) return msgDefault;
                return translated;
              })()}
            </CardText>
          </div>
        </Modal>
      )}
    </div>
  );
};

export default DynamicForm;
