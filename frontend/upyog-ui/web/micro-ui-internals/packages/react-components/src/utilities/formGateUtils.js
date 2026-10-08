/**
 * formGateUtils.js
 *
 * Resolves the optional registration / lookup gate used by DynamicForm:
 * a top-of-form choice that unlocks a search field or redirects to create a
 * related record, then reveals the rest of the form.
 *
 * The gate comes only from `routeConfig.formGate` (MDMS JSON and/or the
 * module's local config). Field names, redirect copy, and reset lists stay
 * in that module config. Estate still has a built-in fallback when a form
 * has `assetRegistrationType` and no `formGate` of its own.
 *
 * @see DynamicForm
 */

/** Estate NewRegistration defaults (existing asset vs new building). */
export const ESTATE_FORM_GATE = {
  typeField: "assetRegistrationType",
  detailsField: "showRegistrationDetails",
  searchValue: "EXISTING_ASSET",
  openValue: "NEW_BUILDING",
  searchField: "searchEstateNo",
  redirectOnValue: null,
  redirectModalHeading: "EST_CREATE_NEW_REGISTRATION",
  redirectModalMessage: "EST_CREATE_NEW_REGISTRATION_REDIRECT_INFO",
  notFoundError: "EST_ASSET_NOT_FOUND",
};

/**
 * Rewrite a hardcoded `/upyog-ui` path when the shell sets a different
 * `window.contextPath`. Leaves the path unchanged for the default shell.
 *
 * @param {string} path
 * @returns {string}
 */
export const resolveAppPath = (path) => {
  if (!path || typeof path !== "string") return path;
  const raw = typeof window !== "undefined" ? window.contextPath : "";
  const ctx = String(raw || "").replace(/^\/+|\/+$/g, "");
  if (!ctx || ctx === "upyog-ui") return path;
  if (path === "/upyog-ui") return `/${ctx}`;
  if (path.startsWith("/upyog-ui/")) return `/${ctx}${path.slice("/upyog-ui".length)}`;
  return path;
};

/**
 * Resolve the active form gate for a DynamicForm route.
 *
 * @param {object}   [routeConfig]
 * @param {object[]} [flatFields] Flattened leaf field configs.
 * @returns {object|null} Gate config, or null when the form has no gate.
 */
export const resolveFormGate = (routeConfig = {}, flatFields = []) => {
  if (routeConfig?.formGate && typeof routeConfig.formGate === "object") {
    const { _comment, ...gate } = routeConfig.formGate;
    return gate.typeField ? gate : null;
  }

  const names = new Set(
    (flatFields || []).map((fc) => fc?.field?.name).filter(Boolean)
  );
  if (names.has("assetRegistrationType")) return { ...ESTATE_FORM_GATE };
  return null;
};

/**
 * Find the first createNewPath declared on any field in the form
 * (search field first, then any other field).
 *
 * @param {object[]} flatFields
 * @param {string}   [preferredFieldName]
 * @returns {string|null}
 */
export const resolveCreateNewPath = (flatFields = [], preferredFieldName) => {
  if (preferredFieldName) {
    const preferred = flatFields.find((fc) => fc?.field?.name === preferredFieldName);
    if (preferred?.field?.createNewPath) return preferred.field.createNewPath;
  }
  const any = flatFields.find((fc) => fc?.field?.createNewPath);
  return any?.field?.createNewPath || null;
};

/**
 * Whether the wizard ActionBar should show for a gated form.
 * Details unlocked (YES) → show. Open-path gate value → show.
 * TL movable trades set detailsField without using the property gate.
 *
 * @param {object|null} formGate
 * @param {object}      formData
 * @returns {boolean}
 */
export const shouldShowGatedActionBar = (formGate, formData = {}) => {
  if (!formGate) return true;
  const details = String(formData[formGate.detailsField] || "").toUpperCase();
  if (details === "YES") return true;
  if (
    formGate.openValue &&
    String(formData[formGate.typeField] || "").toUpperCase() ===
      String(formGate.openValue).toUpperCase()
  ) {
    return true;
  }
  // TL movable: structure chosen, details already unlocked above.
  // IMMOVABLE without property keeps details empty → hide action bar.
  return false;
};
